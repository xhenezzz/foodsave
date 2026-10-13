import test from "node:test";
import assert from "node:assert/strict";
import { parseImpact } from "../src/impact.ts";

test("валидная статистика backend сохраняется", () => {
  assert.deepEqual(parseImpact({foodSavedKg:1.2,moneySaved:2000,co2SavedKg:3.5}),
    {foodSavedKg:1.2,moneySaved:2000,co2SavedKg:3.5});
});
test("пустые нули корректны для пользователя без заказов", () => {
  assert.deepEqual(parseImpact({foodSavedKg:0,moneySaved:0,co2SavedKg:0}),
    {foodSavedKg:0,moneySaved:0,co2SavedKg:0});
});
test("числа, переданные строками, нормализуются", () => {
  assert.deepEqual(parseImpact({foodSavedKg:"1.25",moneySaved:"500",co2SavedKg:"0"}),
    {foodSavedKg:1.25,moneySaved:500,co2SavedKg:0});
});
test("неполный ответ отклоняем вместо отображения undefined", () => {
  assert.throws(()=>parseImpact({foodSavedKg:1,moneySaved:3}),/co2SavedKg/);
  assert.throws(()=>parseImpact({}),/foodSavedKg/);
});
test("ошибочные числа и null отклоняются", () => {
  for(const bad of [null,undefined,[],{foodSavedKg:-1,moneySaved:2,co2SavedKg:4},{foodSavedKg:NaN,moneySaved:1,co2SavedKg:1}]){
    assert.throws(()=>parseImpact(bad));
  }
});
