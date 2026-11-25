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

test("модерация: заявки и объявления одобряются и отклоняются с причиной", async () => {
  const calls = await capture(async () => {
    await api.admin.applications("PENDING");
    await api.admin.approveApplication("b1");
    await api.admin.rejectApplication("b2", "нет номера дома");
    await api.admin.listings({moderation: "PENDING"});
    await api.admin.approveListing("l1");
    await api.admin.rejectListing("l2", "фото не соответствует");
  });
  assert.deepEqual(calls.map(c => `${c.method} ${c.url}`), [
    "GET http://localhost:8081/api/admin/applications?status=PENDING",
    "POST http://localhost:8081/api/admin/applications/b1/approve",
    "POST http://localhost:8081/api/admin/applications/b2/reject",
    "GET http://localhost:8081/api/admin/listings?moderation=PENDING",
    "POST http://localhost:8081/api/admin/listings/l1/approve",
    "POST http://localhost:8081/api/admin/listings/l2/reject"
  ]);
  assert.deepEqual(JSON.parse(calls[2].body), { reason: "нет номера дома" });
  assert.deepEqual(JSON.parse(calls[5].body), { reason: "фото не соответствует" });
});

test("заявка бизнеса отправляет тип, контакты и точку на карте", async () => {
  const application = { businessName: "Пекарня Нан", businessType: "BAKERY", description: "", address: "ул. Абая 10",
    latitude: 43.24, longitude: 76.9, contactPhone: "+77001234567", bin: "" };
  const calls = await capture(async () => {
    await api.createBusinessProfile(application);
    await api.updateBusinessProfile(application);
  });
  assert.equal(calls[0].method, "POST");
  assert.equal(calls[1].method, "PUT");
  assert.deepEqual(JSON.parse(calls[0].body), application);
});
