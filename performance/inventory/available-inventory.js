import http from 'k6/http';
import { check, sleep } from 'k6';

// Giới hạn baseline để cấu hình sai không tạo tải đột biến ngoài dự kiến.
const requestedPeakVUs = Number.parseInt(__ENV.K6_PEAK_VUS || '20', 10);
const peakVUs = Number.isFinite(requestedPeakVUs)
  ? Math.min(20, Math.max(1, requestedPeakVUs))
  : 20;
const readPath = __ENV.INVENTORY_READ_PATH || '/inventory/available?page=0&size=50';

export const options = {
  stages: [
    { duration: '30s', target: Math.max(1, Math.ceil(peakVUs / 2)) },
    { duration: '2m', target: peakVUs },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    'http_req_failed{api:inventory_available}': ['rate<0.01'],
    'http_req_duration{api:inventory_available}': ['p(95)<750', 'p(99)<1500'],
    checks: ['rate>0.99'],
  },
  tags: { service: 'inventory-service' },
};

export function setup() {
  const tokenUrl = __ENV.AUTH_TOKEN_URL;
  const clientId = __ENV.AUTH_CLIENT_ID;
  const clientSecret = __ENV.AUTH_CLIENT_SECRET;
  const baseUrl = __ENV.INVENTORY_BASE_URL;

  if (!tokenUrl || !clientId || !clientSecret || !baseUrl) {
    throw new Error('Thiếu AUTH_TOKEN_URL, AUTH_CLIENT_ID, AUTH_CLIENT_SECRET hoặc INVENTORY_BASE_URL');
  }

  const fields = [
    ['grant_type', 'client_credentials'],
    ['client_id', clientId],
    ['client_secret', clientSecret],
  ];
  if (__ENV.AUTH_CLIENT_SCOPE) fields.push(['scope', __ENV.AUTH_CLIENT_SCOPE]);
  const body = fields.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&');
  const tokenResponse = http.post(tokenUrl, body, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    timeout: '15s',
  });
  const tokenJson = tokenResponse.json();
  if (tokenResponse.status !== 200 || !tokenJson || !tokenJson.access_token) {
    throw new Error(`Không lấy được access token từ identity provider; HTTP ${tokenResponse.status}`);
  }

  const normalizedBaseUrl = baseUrl.replace(/\/$/, '');
  const normalizedPath = readPath.startsWith('/') ? readPath : `/${readPath}`;
  return { accessToken: tokenJson.access_token, inventoryUrl: `${normalizedBaseUrl}${normalizedPath}` };
}

export default function (data) {
  const response = http.get(data.inventoryUrl, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { api: 'inventory_available' },
    timeout: '10s',
  });

  let body;
  try {
    body = response.json();
  } catch (_) {
    body = null;
  }

  check(response, {
    'Inventory trả HTTP 200': (res) => res.status === 200,
    'ApiResponse báo thành công': () => body !== null && body.success === true,
  });
  sleep(1);
}
