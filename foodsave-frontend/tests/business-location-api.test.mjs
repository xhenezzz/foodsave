import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.ts';

test('создание и обновление бизнес-профиля отправляют выбранные координаты', async () => {
  const previousFetch = globalThis.fetch;
  const previousStorage = globalThis.localStorage;
  const requests = [];
  globalThis.localStorage = { getItem: () => 'test-jwt' };
  globalThis.fetch = async (url, options) => {
    requests.push({ url: String(url), method: options.method, body: JSON.parse(options.body) });
    return new Response(JSON.stringify({
      id: 'business-1',
      userId: 'user-1',
      businessName: 'Test Cafe',
      description: 'Test',
      address: 'Abay 10',
      latitude: 43.238949,
      longitude: 76.889709
    }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };

  const data = {
    businessName: 'Test Cafe',
    description: 'Test',
    address: 'Abay 10',
    latitude: 43.238949,
    longitude: 76.889709
  };

  try {
    await api.createBusinessProfile(data);
    await api.updateBusinessProfile(data);

    assert.equal(requests[0].url, 'http://localhost:8081/api/business/profile');
    assert.equal(requests[0].method, 'POST');
    assert.equal(requests[1].url, 'http://localhost:8081/api/business/profile');
    assert.equal(requests[1].method, 'PUT');

    for (const request of requests) {
      assert.equal(request.body.latitude, data.latitude);
      assert.equal(request.body.longitude, data.longitude);
      assert.equal(request.body.businessName, data.businessName);
    }
  } finally {
    globalThis.fetch = previousFetch;
    globalThis.localStorage = previousStorage;
  }
});

test('геокодирование адреса и выбранной точки использует отдельные GET-запросы', async () => {
  const previousFetch = globalThis.fetch;
  const previousStorage = globalThis.localStorage;
  const requests = [];
  globalThis.localStorage = { getItem: () => 'test-jwt' };
  globalThis.fetch = async (url, options) => {
    requests.push({ url: String(url), method: options.method ?? 'GET' });
    return new Response(JSON.stringify({
      address: 'ул. Абая, 10',
      latitude: 43.238949,
      longitude: 76.889709
    }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };

  try {
    await api.geocodeBusinessAddress('ул. Абая, 10');
    await api.reverseGeocodeBusinessLocation(43.238949, 76.889709);

    assert.equal(requests[0].url, 'http://localhost:8081/api/business/profile/geocode?address=%D1%83%D0%BB.%20%D0%90%D0%B1%D0%B0%D1%8F%2C%2010');
    assert.equal(requests[0].method, 'GET');
    assert.equal(requests[1].url, 'http://localhost:8081/api/business/profile/reverse-geocode?latitude=43.238949&longitude=76.889709');
    assert.equal(requests[1].method, 'GET');
  } finally {
    globalThis.fetch = previousFetch;
    globalThis.localStorage = previousStorage;
  }
});
