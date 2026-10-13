import type { Impact } from "./types";

// Отсеиваем несовместимый ответ API вместо показа «undefined» в статистике.
export function parseImpact(value: unknown): Impact {
  if (!value || typeof value !== "object") {
    throw new Error("Некорректный ответ /api/impact/my: ожидался объект со статистикой");
  }
  const candidate = value as Record<string, unknown>;
  const fields = ["foodSavedKg", "moneySaved", "co2SavedKg"] as const;
  const parsed: Record<string, number> = {};
  for (const key of fields) {
    const raw = candidate[key];
    const n = typeof raw === "number" ? raw : typeof raw === "string" && raw.trim() !== "" ? Number(raw) : NaN;
    if (!Number.isFinite(n) || n < 0) {
      throw new Error(`Ответ /api/impact/my не содержит корректного поля ${key}`);
    }
    parsed[key] = n;
  }
  return parsed as unknown as Impact;
}
