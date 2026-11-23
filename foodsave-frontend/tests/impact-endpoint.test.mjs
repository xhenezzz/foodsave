import test from "node:test";
import assert from "node:assert/strict";
import { api } from "../src/api.ts";

test("API статистики вызывает реальный Spring Boot endpoint /api/impact/me", async () => {
  const prevFetch = globalThis.fetch;
  const prevStorage = globalThis.localStorage;
  let called = false;
  try {
    globalThis.localStorage = { getItem: () => "test-jwt" };
    globalThis.fetch = async (url, options) => {
      called = true;
      assert.equal(url, "http://localhost:8081/api/impact/me");
      assert.equal(options.headers.get("Authorization"), "Bearer test-jwt");
      return { ok:true, text:async() => JSON.stringify({
        foodSavedKg:1.5, moneySaved:2200, co2SavedKg:3.75
      })};
    };
    const result = await api.getImpact();
    assert.equal(called,true);
    assert.deepEqual(result,{foodSavedKg:1.5,moneySaved:2200,co2SavedKg:3.75});
  } finally {
    globalThis.fetch=prevFetch;
    globalThis.localStorage=prevStorage;
  }
});
