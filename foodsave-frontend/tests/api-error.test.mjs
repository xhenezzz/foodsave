import test from "node:test";
import assert from "node:assert/strict";
import { api, ApiError, httpStatus } from "../src/api.ts";

async function withFetch(response, run) {
  const prevFetch = globalThis.fetch;
  const prevStorage = globalThis.localStorage;
  try {
    globalThis.localStorage = { getItem: () => "test-jwt" };
    globalThis.fetch = async () => response;
    await run();
  } finally {
    globalThis.fetch = prevFetch;
    globalThis.localStorage = prevStorage;
  }
}

test("ошибка API сохраняет HTTP-статус и сообщение сервера", async () => {
  await withFetch({ ok:false, status:404, text:async() => JSON.stringify({ message:"Профиль бизнеса не найден" }) }, async () => {
    const error = await api.getBusinessProfile().then(() => null, e => e);
    assert.ok(error instanceof ApiError);
    assert.equal(error.message, "Профиль бизнеса не найден");
    assert.equal(httpStatus(error), 404);
  });
});

test("пустой ответ с ошибкой даёт понятный HTTP-статус", async () => {
  await withFetch({ ok:false, status:403, text:async() => "" }, async () => {
    const error = await api.getProfile().then(() => null, e => e);
    assert.equal(httpStatus(error), 403);
    assert.equal(error.message, "HTTP 403");
  });
});

test("httpStatus не выдумывает статус для обычных ошибок", () => {
  assert.equal(httpStatus(new Error("network down")), null);
});
