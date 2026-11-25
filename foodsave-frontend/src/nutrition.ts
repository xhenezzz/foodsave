import type { Nutrition } from "./types";

export const NUTRITION_FIELDS = [
  {key: "calories", label: "Калории", unit: "ккал"},
  {key: "proteins", label: "Белки", unit: "г"},
  {key: "fats", label: "Жиры", unit: "г"},
  {key: "carbs", label: "Углеводы", unit: "г"}
] as const;
export type NutritionKey = typeof NUTRITION_FIELDS[number]["key"];

const isValue = (value: number | null | undefined): value is number =>
  typeof value === "number" && Number.isFinite(value);

export const hasNutrition = (n: Nutrition): boolean => NUTRITION_FIELDS.some(({key}) => isValue(n[key]));

export const formatNutritionValue = (value: number | null | undefined): string =>
  isValue(value) ? value.toLocaleString("ru-RU", {maximumFractionDigits: 1}) : "—";

/** "" → null; "12,5" → 12.5; garbage → NaN (the form shows an error). */
export function parseNutritionInput(text: string): number | null {
  const trimmed = text.trim().replace(",", ".");
  if (!trimmed) return null;
  return /^\d+(\.\d{1,2})?$/.test(trimmed) ? Number(trimmed) : Number.NaN;
}

/** Same rules as ListingRules.validateNutrition on the backend. */
export function nutritionError(n: Nutrition): string | null {
  for (const {key, label} of NUTRITION_FIELDS) {
    const value = n[key];
    if (value === null || value === undefined) continue;
    if (Number.isNaN(value)) return `${label}: введи число, до двух знаков после запятой`;
    if (value < 0) return `${label} не могут быть отрицательными`;
  }
  if (isValue(n.calories) && n.calories > 900) return "Калорийность — не больше 900 ккал на 100 г";
  const macros = [n.proteins, n.fats, n.carbs].filter(isValue);
  if (macros.some(v => v > 100)) return "Белки, жиры и углеводы — не больше 100 г на 100 г продукта";
  if (macros.reduce((sum, v) => sum + v, 0) > 100) return "Сумма белков, жиров и углеводов не может превышать 100 г";
  return null;
}

/**
 * Soft check: calories should be close to 4·P + 9·F + 4·C.
 * Returns the expected value when the label looks inconsistent, otherwise null.
 */
export function caloriesMismatch(n: Nutrition): number | null {
  if (![n.calories, n.proteins, n.fats, n.carbs].every(isValue)) return null;
  const expected = 4 * n.proteins! + 9 * n.fats! + 4 * n.carbs!;
  const diff = Math.abs(n.calories! - expected);
  return diff > 30 && diff > expected * 0.2 ? Math.round(expected) : null;
}
