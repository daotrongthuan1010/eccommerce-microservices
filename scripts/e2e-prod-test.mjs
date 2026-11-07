#!/usr/bin/env node
/**
 * E2E test luong chinh tren PRODUCTION that (https://shop-hatd.online).
 * Chuoi 8 buoc theo docs memory e2e-payment-flow:
 *   login -> cart -> variant -> add item -> checkout (Idempotency-Key)
 *   -> payment tao (poll) -> fake-gateway pay -> order PAID
 *
 * Chay: node scripts/e2e-prod-test.mjs
 * Yeu cau: Node 18+. Test account: thuan.user (Secret ngoai repo).
 *
 * §TEST_COVERAGE_REPORT P0: saga E2E tren production — bay gio co script
 * chay duoc ca local lan manual.
 */

const BASE = process.env.E2E_BASE_URL || 'https://shop-hatd.online';
const USERNAME = process.env.E2E_USER || 'thuan.user';
const PASSWORD = process.env.E2E_PASSWORD; // KHONG hardcode — env var

// §TLS: shop-hatd.online dung cert-manager self-signed — Node fetch tu choi.
// E2E_ACCEPT_SELF_SIGNED=1 (chi dung cho test) hoac set NODE_EXTRA_CA_CERTS.
if (process.env.E2E_ACCEPT_SELF_SIGNED === '1') {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}

const results = [];
function record(step, ok, detail = '') {
  results.push({ step, ok, detail });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${step}${detail ? ' — ' + detail : ''}`);
}

async function api(method, path, { token, body, headers = {}, raw = false } = {}) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* raw */ }
  return { status: res.status, json, text, data: json?.data ?? json };
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function poll(fn, { tries = 20, interval = 3000, name = 'poll' } = {}) {
  for (let i = 0; i < tries; i++) {
    const v = await fn();
    if (v) return v;
    process.stdout.write(`  ...${name} ${i + 1}/${tries}\r`);
    await sleep(interval);
  }
  return null;
}

async function main() {
  if (!PASSWORD) {
    console.error('Thieu E2E_PASSWORD. Usage: E2E_PASSWORD=... node scripts/e2e-prod-test.mjs');
    process.exit(2);
  }

  // 1. LOGIN
  const login = await api('POST', '/api/auth/login',
    { body: { username: USERNAME, password: PASSWORD } });
  const token = login.data?.access_token ?? login.json?.access_token;
  record('1. Login /api/auth/login', !!token, `status=${login.status}`);
  if (!token) process.exit(1);

  // 2. GET CART
  const cart = await api('GET', '/api/order/cart', { token });
  record('2. GET /api/order/cart', cart.status === 200, `status=${cart.status}`);

  // 3. TIM VARIANT (SKU) — browse endpoint (gateway permitAll, co envelope)
  const products = await api('GET', '/api/product/products/browse?page=0&size=5');
  const first = products.data?.content?.[0];
  const productId = first?.id;
  record('3. GET /api/product/products/browse', !!productId, `productId=${productId} name=${first?.productCode}`);

  let skuId = null, productName = 'E2E Test Product', unitPrice = 100000;
  if (productId) {
    const variants = await api('GET', `/api/product/products-variant/products/${productId}/variants`);
    const v = Array.isArray(variants.data) ? variants.data[0] : variants.data?.content?.[0] ?? variants.json?.[0];
    skuId = v?.id ?? v?.skuId ?? v?.variantId;
    if (v?.name) productName = String(v.name).replace(/[^\x00-\x7F]/g, 'A'); // §ASCII: shell/JSON unicode bug
    if (v?.price ?? v?.unitPrice) unitPrice = v.price ?? v.unitPrice;
  }
  record('4. GET variants -> skuId', !!skuId, `skuId=${skuId} price=${unitPrice}`);

  // 5. ADD TO CART
  const add = await api('POST', '/api/order/cart/items', {
    token,
    body: { skuId, productName, unitPrice, quantity: 1 },
  });
  record('5. POST /api/order/cart/items', add.status === 200 || add.status === 201, `status=${add.status}`);

  // 6. CHECKOUT + Idempotency-Key
  const idemKey = `e2e-${Date.now()}`;
  const checkout = await api('POST', '/api/order/me/checkout', {
    token,
    headers: { 'Idempotency-Key': idemKey },
    body: { shippingAddress: 'E2E Test Address', recipientName: 'E2E Tester', recipientPhone: '0900000000', paymentMethod: 'VNPAY' },
  });
  const order = checkout.data ?? checkout.json;
  const orderId = order?.orderId ?? order?.id;
  record('6. POST /api/order/me/checkout', !!orderId, `orderId=${orderId} status=${order?.status}`);

  // 7. PAYMENT duoc tao tu event OrderCreated (poll by-order)
  const payment = await poll(async () => {
    const r = await api('GET', `/api/payment/payments/by-order/${orderId}`, { token });
    return r.status === 200 && r.data ? r.data : null;
  }, { name: 'payment-created' });
  record('7. Payment tao tu OrderCreated event', !!payment, `paymentId=${payment?.id} status=${payment?.status}`);

  // 8. FAKE GATEWAY thanh toan -> IPN -> order PAID (poll)
  // GET /pay chi tra HTML form; phai POST /pay/{txnRef}/complete?outcome=SUCCESS
  let checkoutUrl = payment?.checkoutUrl;
  if (checkoutUrl) {
    const txnRef = new URL(checkoutUrl).searchParams.get('txnRef');
    const complete = await fetch(`${BASE}/fake-gateway/fake/vnpay/pay/${encodeURIComponent(txnRef)}/complete?outcome=SUCCESS`,
      { method: 'POST', redirect: 'manual' });
    record('8a. Fake-gateway pay SUCCESS', complete.status === 302 || complete.status === 200,
      `status=${complete.status} txnRef=${txnRef}`);
  } else {
    record('8a. Fake-gateway pay', false, 'khong co checkoutUrl');
  }

  const paid = await poll(async () => {
    const r = await api('GET', `/api/order/me/orders/${orderId}`, { token });
    return r.data?.status === 'PAID' ? r.data : null;
  }, { tries: 15, name: 'order-PAID' });
  record('8b. Order PAID sau IPN', !!paid, `status=${paid?.status ?? 'timeout'}`);

  // TONG KET
  const failed = results.filter(r => !r.ok);
  console.log(`\n=== E2E: ${results.length - failed.length}/${results.length} PASS ===`);
  if (failed.length) {
    console.log('FAILED steps:', failed.map(f => f.step).join('; '));
    process.exit(1);
  }
}

main().catch((e) => { console.error('E2E CRASH:', e.message); process.exit(1); });
