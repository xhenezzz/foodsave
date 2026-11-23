import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.ts';

test('verifyEmail receives AuthResponse with JWT from the POST endpoint', async () => {
  const previousFetch = globalThis.fetch;
  const previousStorage = globalThis.localStorage;
  let request;
  globalThis.localStorage = { getItem: () => null };
  globalThis.fetch = async (url, options) => {
    request = { url: String(url), method: options.method };
    return new Response(JSON.stringify({ token: 'verified-test-jwt' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  try {
    const result = await api.verifyEmail('raw token/+?');
    assert.equal(result.token, 'verified-test-jwt');
    assert.equal(request.url, 'http://localhost:8081/api/auth/verify-email?token=raw%20token%2F%2B%3F');
    assert.equal(request.method, 'POST');
  } finally {
    globalThis.fetch = previousFetch;
    globalThis.localStorage = previousStorage;
  }
});
