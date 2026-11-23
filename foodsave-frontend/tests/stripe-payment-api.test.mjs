import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.ts';

test('startStripeCheckout requests an embedded Stripe session for the authenticated order', async () => {
  const originalFetch = globalThis.fetch;
  const originalStorage = globalThis.localStorage;
  const calls = [];
  globalThis.localStorage = { getItem: () => 'fake-test-jwt' };
  globalThis.fetch = async (url, opts) => {
    calls.push({ url: String(url), method: opts.method, auth: opts.headers.get('Authorization') });
    return new Response(JSON.stringify({
      paymentId: 'pay-1', orderId: 'ord-1', amount: 1200,
      currency: 'KZT', status: 'PENDING', clientSecret: 'cs_test_secret_for_test_only'
    }), { status: 200 });
  };
  try {
    const session = await api.startStripeCheckout('ord-1');
    assert.equal(session.status, 'PENDING');
    assert.equal(session.currency, 'KZT');
    assert.equal(session.clientSecret, 'cs_test_secret_for_test_only');
    assert.equal(calls[0].url, 'http://localhost:8081/api/payments/ord-1/stripe/checkout-session');
    assert.equal(calls[0].method, 'POST');
    assert.equal(calls[0].auth, 'Bearer fake-test-jwt');
  } finally {
    globalThis.fetch = originalFetch;
    globalThis.localStorage = originalStorage;
  }
});

test('getPaymentStatus reads server-side payment state after returning from Stripe', async () => {
  const previousFetch = globalThis.fetch;
  const previousStorage = globalThis.localStorage;
  globalThis.localStorage = { getItem: () => 'fake-test-jwt' };
  let request;
  globalThis.fetch = async (url, opts) => {
    request = { url: String(url), method: opts.method, auth: opts.headers.get('Authorization') };
    return new Response(JSON.stringify({
      id: 'pay-2', orderId: 'ord-2', amount: 2500, status: 'SUCCESS', createdAt: '2026-11-16T12:00:00'
    }), { status: 200 });
  };
  try {
    const result = await api.getPaymentStatus('ord-2');
    assert.equal(result.status, 'SUCCESS');
    assert.equal(request.url, 'http://localhost:8081/api/payments/ord-2');
    assert.equal(request.method, undefined);
    assert.equal(request.auth, 'Bearer fake-test-jwt');
  } finally {
    globalThis.fetch = previousFetch;
    globalThis.localStorage = previousStorage;
  }
});
