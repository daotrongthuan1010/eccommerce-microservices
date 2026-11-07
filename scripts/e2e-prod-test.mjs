#!/usr/bin/env node
/**
 * E2E test suite tren PRODUCTION that (https://shop-hatd.online).
 *
 * Cases:
 *  - happy-path: login -> cart -> browse -> checkout -> payment -> pay -> PAID
 *  - idempotency: checkout 2 lan cung key -> cung orderId, khong don thu 2
 *  - saga-compensation: pay FAILED -> order PAYMENT_FAILED (event chain that)
 *  - auth: sai password -> 401
 *  - search: Elasticsearch query qua gateway
 *
 * Chay: E2E_ACCEPT_SELF_SIGNED=1 E2E_PASSWORD=... node scripts/e2e-prod-test.mjs
 * Yeu cau: Node 18+. Test account: thuan.user (Secret ngoai repo).
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

async function api(method, path, { token, body, headers = {} } = {}) {
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

async function login() {
  const r = await api('POST', '/api/auth/login',
    { body: { username: USERNAME, password: PASSWORD } });
  return r.data?.access_token ?? r.json?.access_token;
}

async function pickSku() {
  const products = await api('GET', '/api/product/products/browse?page=0&size=5');
  const first = products.data?.content?.[0];
  if (!first?.id) return null;
  const variants = await api('GET', `/api/product/products-variant/products/${first.id}/variants`);
  const v = Array.isArray(variants.data) ? variants.data[0]
    : variants.data?.content?.[0] ?? variants.json?.[0];
  return {
    skuId: v?.id ?? v?.skuId ?? v?.variantId,
    productName: v?.name ? String(v.name).replace(/[^\x00-\x7F]/g, 'A') : 'E2E Test Product',
    unitPrice: v?.price ?? v?.unitPrice ?? 100000,
  };
}

async function addToCart(token, sku) {
  return api('POST', '/api/order/cart/items', {
    token, body: { skuId: sku.skuId, productName: sku.productName, unitPrice: sku.unitPrice, quantity: 1 },
  });
}

async function checkout(token, idemKey, method = 'VNPAY') {
  return api('POST', '/api/order/me/checkout', {
    token,
    headers: { 'Idempotency-Key': idemKey },
    body: { shippingAddress: 'E2E Test Address', recipientName: 'E2E Tester', recipientPhone: '0900000000', paymentMethod: method },
  });
}

async function completeGatewayPayment(payment, outcome) {
  const txnRef = new URL(payment.checkoutUrl).searchParams.get('txnRef');
  const r = await fetch(
    `${BASE}/fake-gateway/fake/vnpay/pay/${encodeURIComponent(txnRef)}/complete?outcome=${outcome}`,
    { method: 'POST', redirect: 'manual' });
  return r.status === 302 || r.status === 200;
}

// ============================================================ CASES

async function caseHappyPath(token) {
  console.log('\n--- CASE 1: Happy path checkout -> payment -> PAID ---');
  const sku = await pickSku();
  record('1.1 Browse + variant', !!sku?.skuId, `skuId=${sku?.skuId}`);
  if (!sku?.skuId) return;

  const add = await addToCart(token, sku);
  record('1.1b Add to cart', add.status === 200 || add.status === 201, `status=${add.status} ${add.status >= 400 ? add.text.slice(0, 150) : ''}`);
  const co = await checkout(token, `e2e-happy-${Date.now()}`);
  const orderId = co.data?.orderId;
  record('1.2 Checkout', !!orderId, `orderId=${orderId} status=${co.status} ${!orderId ? co.text.slice(0, 250) : ''}`);
  if (!orderId) return;

  // Poll den khi payment co checkoutUrl (session gateway da tao)
  const payment = await poll(async () => {
    const r = await api('GET', `/api/payment/payments/by-order/${orderId}`, { token });
    return (r.status === 200 && r.data?.checkoutUrl) ? r.data : null;
  }, { name: 'payment-created' });
  record('1.3 Payment tu event OrderCreated', !!payment, `status=${payment?.status}`);

  const paid = await completeGatewayPayment(payment, 'SUCCESS');
  record('1.4 Gateway pay SUCCESS', paid);

  const order = await poll(async () => {
    const r = await api('GET', `/api/order/me/orders/${orderId}`, { token });
    return r.data?.status === 'PAID' ? r.data : null;
  }, { tries: 15, name: 'order-PAID' });
  record('1.5 Order PAID sau IPN', !!order, `status=${order?.status ?? 'timeout'}`);
}

async function caseIdempotencyReplay(token) {
  console.log('\n--- CASE 2: Idempotency — checkout 2 lan cung key ---');
  const sku = await pickSku();
  if (!sku?.skuId) { record('2.x', false, 'khong co sku'); return; }
  await addToCart(token, sku);

  const key = `e2e-idem-${Date.now()}`;
  const first = await checkout(token, key);
  await sleep(1500); // checkout 1 clear cart — replay doc ket qua luu, khong can cart
  const second = await checkout(token, key); // replay: cung key, cung payload
  const id1 = first.data?.orderId, id2 = second.data?.orderId;
  record('2.1 Replay cung key -> cung orderId', id1 && id1 === id2,
    `1=${id1} 2=${id2} s2=${second.status} ${!id2 ? second.text.slice(0, 150) : ''}`);

  // Khac payload cung key -> phai tu choi (409/422)
  const conflict = await checkout(token, key + 'x', 'COD'); // key moi, method khac — khong phai conflict
  // That su: cung key nhung doi dia chi
  const conflictReq = await api('POST', '/api/order/me/checkout', {
    token, headers: { 'Idempotency-Key': key },
    body: { shippingAddress: 'DIA CHI KHAC', recipientName: 'E2E Tester', recipientPhone: '0900000000', paymentMethod: 'VNPAY' },
  });
  record('2.2 Cung key khac payload -> tu choi',
    conflictReq.status === 409 || conflictReq.status === 422, `status=${conflictReq.status}`);
}

async function caseSagaCompensation(token) {
  console.log('\n--- CASE 3: Saga compensation — pay FAILED -> order PAYMENT_FAILED ---');
  const sku = await pickSku();
  if (!sku?.skuId) { record('3.x', false, 'khong co sku'); return; }
  await addToCart(token, sku);

  const co = await checkout(token, `e2e-fail-${Date.now()}`);
  const orderId = co.data?.orderId;
  record('3.1 Checkout', !!orderId, `orderId=${orderId} s=${co.status} ${!orderId ? co.text.slice(0, 150) : ''}`);
  if (!orderId) return;

  // Poll den khi co checkoutUrl — gateway session phai san sang
  const payment = await poll(async () => {
    const r = await api('GET', `/api/payment/payments/by-order/${orderId}`, { token });
    return (r.status === 200 && r.data?.checkoutUrl) ? r.data : null;
  }, { name: 'payment-created' });
  record('3.2 Payment tao', !!payment);

  const failed = await completeGatewayPayment(payment, 'FAILED');
  record('3.3 Gateway pay FAILED', failed);

  // §ADR-0001: order phai chuyen PAYMENT_FAILED tu event PaymentFailed
  const order = await poll(async () => {
    const r = await api('GET', `/api/order/me/orders/${orderId}`, { token });
    return r.data?.status === 'PAYMENT_FAILED' ? r.data : null;
  }, { tries: 15, name: 'order-FAILED' });
  record('3.4 Order PAYMENT_FAILED (saga event chain)', !!order, `status=${order?.status ?? 'timeout'}`);
}

async function caseAuth() {
  console.log('\n--- CASE 4: Auth — sai password -> 401 ---');
  const r = await api('POST', '/api/auth/login',
    { body: { username: USERNAME, password: 'sai-mat-khau-roi' } });
  record('4.1 Sai password -> tu choi', r.status === 401 || r.status === 400, `status=${r.status}`);

  const noAuth = await api('GET', '/api/order/cart');
  record('4.2 Khong token -> 401/403', noAuth.status === 401 || noAuth.status === 403, `status=${noAuth.status}`);
}

async function caseSearch(token) {
  console.log('\n--- CASE 5: Search Elasticsearch qua gateway ---');
  const r = await api('GET', '/api/search/products?q=iphone&size=2', { token });
  const hasResults = r.status === 200 && (r.data?.content?.length > 0 || Array.isArray(r.data));
  record('5.1 GET /api/search/products?q=iphone', hasResults,
    `status=${r.status} hits=${r.data?.content?.length ?? 0}`);
}

// ============================================================ MAIN

async function main() {
  if (!PASSWORD) {
    console.error('Thieu E2E_PASSWORD. Usage: E2E_ACCEPT_SELF_SIGNED=1 E2E_PASSWORD=... node scripts/e2e-prod-test.mjs');
    process.exit(2);
  }

  const token = await login();
  record('0. Login', !!token);
  if (!token) process.exit(1);

  await caseHappyPath(token);
  await caseIdempotencyReplay(token);
  await caseSagaCompensation(token);
  await caseAuth();
  await caseSearch(token);

  const failed = results.filter(r => !r.ok);
  console.log(`\n=== E2E: ${results.length - failed.length}/${results.length} PASS ===`);
  if (failed.length) {
    console.log('FAILED:', failed.map(f => f.step).join('; '));
    process.exit(1);
  }
}

main().catch((e) => { console.error('E2E CRASH:', e.message); process.exit(1); });
