import test from "node:test";
import assert from "node:assert/strict";
import { api } from "../src/api.ts";

async function capture(run) {
  const prevFetch = globalThis.fetch;
  const prevStorage = globalThis.localStorage;
  const calls = [];
  try {
    globalThis.localStorage = { getItem: () => "admin-jwt" };
    globalThis.fetch = async (url, options = {}) => {
      calls.push({ url, method: options.method ?? "GET", body: options.body, auth: options.headers.get("Authorization") });
      return { ok: true, text: async () => "[]" };
    };
    await run();
  } finally {
    globalThis.fetch = prevFetch;
    globalThis.localStorage = prevStorage;
  }
  return calls;
}

test("поиск пользователей передаёт только заполненные фильтры", async () => {
  const calls = await capture(async () => {
    await api.admin.users();
    await api.admin.users({ query: "  anna ", role: "BUSINESS" });
    await api.admin.users({ query: "   ", role: "" });
  });
  assert.equal(calls[0].url, "http://localhost:8081/api/admin/users");
  assert.equal(calls[1].url, "http://localhost:8081/api/admin/users?query=anna&role=BUSINESS");
  assert.equal(calls[2].url, "http://localhost:8081/api/admin/users");
  assert.equal(calls[0].auth, "Bearer admin-jwt");
});

test("блокировка отправляет флаг и причину через PUT", async () => {
  const calls = await capture(() => api.admin.setBlocked("u1", true, "спам"));
  assert.equal(calls[0].url, "http://localhost:8081/api/admin/users/u1/blocked");
  assert.equal(calls[0].method, "PUT");
  assert.deepEqual(JSON.parse(calls[0].body), { blocked: true, reason: "спам" });
});

test("модерация и заказы вызывают свои endpoints", async () => {
  const calls = await capture(async () => {
    await api.admin.cancelListing("l1");
    await api.admin.orders("NO_SHOW");
    await api.admin.listings({ status: "ACTIVE" });
  });
  assert.deepEqual(calls.map(c => `${c.method} ${c.url}`), [
    "POST http://localhost:8081/api/admin/listings/l1/cancel",
    "GET http://localhost:8081/api/admin/orders?status=NO_SHOW",
    "GET http://localhost:8081/api/admin/listings?status=ACTIVE"
  ]);
});
