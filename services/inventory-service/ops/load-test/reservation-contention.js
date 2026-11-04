import http from 'k6/http';
import { check, sleep } from 'k6';

// Kịch bản ghi chỉ dùng với fixture staging riêng, không dùng inventory đang bán thật.
const requestedVUs = Number.parseInt(__ENV.K6_PEAK_VUS || '10', 10);
// Có thể tăng tải từng nấc ở staging; giới hạn cứng 500 VU tránh cấu hình nhầm gây bão request.
const peakVUs = Number.isFinite(requestedVUs) ? Math.min(500, Math.max(1, requestedVUs)) : 10;
const baseUrl = (__ENV.INVENTORY_BASE_URL || '').replace(/\/$/, '');
const inventoryId = __ENV.INVENTORY_ID;
const quantity = Number(__ENV.RESERVATION_QUANTITY || '1');
const requestedThinkTime = Number(__ENV.K6_THINK_TIME_SECONDS || '0.1');
const thinkTimeSeconds = Number.isFinite(requestedThinkTime) ? Math.max(0, requestedThinkTime) : 0.1;

export const options = {
  stages: [
    { duration: __ENV.K6_RAMP_UP || '30s', target: Math.max(1, Math.ceil(peakVUs / 2)) },
    { duration: __ENV.K6_HOLD || '2m', target: peakVUs },
    { duration: __ENV.K6_RAMP_DOWN || '30s', target: 0 },
  ],
  thresholds: {
    'http_req_failed{api:inventory_reservation}': ['rate<0.01'],
    'http_req_duration{api:inventory_reservation}': ['p(95)<1000', 'p(99)<2000'],
    checks: ['rate>0.99'],
  },
  tags: { service: 'inventory-service', workload: 'reservation-contention' },
};

// HTTP 409 vì hết tồn là kết quả nghiệp vụ dự kiến trong bài test tranh chấp.
http.setResponseCallback(http.expectedStatuses(201, 409));

export function setup() {
  const tokenUrl = __ENV.AUTH_TOKEN_URL;
  const clientId = __ENV.AUTH_CLIENT_ID;
  const clientSecret = __ENV.AUTH_CLIENT_SECRET;
  if (__ENV.LOAD_TEST_CONFIRM !== 'STAGING_ONLY') {
    throw new Error('Phải xác nhận LOAD_TEST_CONFIRM=STAGING_ONLY; không chạy kịch bản ghi trên production');
  }
  if (!baseUrl || !inventoryId || !tokenUrl || !clientId || !clientSecret) {
    throw new Error('Thiếu URL, INVENTORY_ID hoặc OAuth client dành riêng cho staging');
  }
  if (!Number.isFinite(quantity) || quantity <= 0) {
    throw new Error('RESERVATION_QUANTITY phải lớn hơn 0');
  }

  const body = [
    ['grant_type', 'client_credentials'],
    ['client_id', clientId],
    ['client_secret', clientSecret],
  ].map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&');
  const tokenResponse = http.post(tokenUrl, body, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    timeout: '15s',
  });
  const token = tokenResponse.json();
  if (tokenResponse.status !== 200 || !token || !token.access_token) {
    throw new Error(`Không lấy được access token; HTTP ${tokenResponse.status}`);
  }
  return { accessToken: token.access_token };
}

export default function (data) {
  // Mỗi vòng là lệnh mới; operationId ổn định nếu hệ thống kiểm thử cấu hình retry phía client.
  const operationId = `load-${__VU}-${__ITER}`;
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();
  const request = JSON.stringify({
    reservationId: `reservation-${operationId}`,
    orderId: `order-${operationId}`,
    operationId,
    expiresAt,
    lines: [{ inventoryId, quantity }],
  });
  const params = {
    headers: {
      Authorization: `Bearer ${data.accessToken}`,
      'Content-Type': 'application/json',
    },
    tags: { api: 'inventory_reservation' },
    timeout: '10s',
  };
  let response = http.post(`${baseUrl}/inventory/reservations`, request, params);

  // Mô phỏng client mất response hoặc gateway lỗi; retry giữ nguyên reservationId và operationId.
  if (response.status === 0 || response.status >= 500) {
    response = http.post(`${baseUrl}/inventory/reservations`, request, params);
  }

  check(response, {
    'reservation tạo thành công hoặc hết tồn có kiểm soát': (res) => res.status === 201 || res.status === 409,
  });
  sleep(thinkTimeSeconds);
}
