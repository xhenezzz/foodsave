import test from "node:test";
import assert from "node:assert/strict";
import { caloriesMismatch, formatNutritionValue, hasNutrition, nutritionError, parseNutritionInput } from "../src/nutrition.ts";

test("ввод КБЖУ: пусто — null, запятая — точка, мусор — ошибка", () => {
  assert.equal(parseNutritionInput("  "), null);
  assert.equal(parseNutritionInput("12,5"), 12.5);
  assert.equal(parseNutritionInput("406"), 406);
  assert.ok(Number.isNaN(parseNutritionInput("12.555")));
  assert.ok(Number.isNaN(parseNutritionInput("abc")));
});

test("правила совпадают с backend", () => {
  assert.equal(nutritionError({}), null);
  assert.equal(nutritionError({calories: 406, proteins: 8.2, fats: 21, carbs: 45.5}), null);
  assert.match(nutritionError({calories: 950}), /900/);
  assert.match(nutritionError({proteins: -1}), /отрицательн/);
  assert.match(nutritionError({proteins: 40, fats: 40, carbs: 30}), /100 г/);
  assert.match(nutritionError({fats: Number.NaN}), /число/);
});

test("предупреждение, если калории не сходятся с 4·Б + 9·Ж + 4·У", () => {
  assert.equal(caloriesMismatch({calories: 406, proteins: 8.2, fats: 21, carbs: 45.5}), null);
  assert.equal(caloriesMismatch({calories: 100, proteins: 8.2, fats: 21, carbs: 45.5}), 404);
  assert.equal(caloriesMismatch({calories: 100}), null, "без полного набора не проверяем");
});

test("отображение КБЖУ", () => {
  assert.equal(hasNutrition({}), false);
  assert.equal(hasNutrition({calories: null, fats: 0}), true);
  assert.equal(formatNutritionValue(null), "—");
  assert.equal(formatNutritionValue(12.5), "12,5");
});
