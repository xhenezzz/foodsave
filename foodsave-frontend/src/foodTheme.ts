/** Food category colours and emoji, shared by covers, chips and illustrations. */
import type { CSSProperties } from "react";
import type { FoodCategory } from "./types";

export type Theme = {label: string; emoji: string; from: string; to: string; ink: string};

export const CATEGORY_THEME: Record<FoodCategory, Theme> = {
  BAKERY:            {label: "Выпечка",          emoji: "🥐", from: "#fff1d6", to: "#ffc98a", ink: "#9a5a12"},
  READY_MEALS:       {label: "Готовые блюда",    emoji: "🍱", from: "#ffe6dc", to: "#ffb497", ink: "#a2442a"},
  DESSERTS:          {label: "Десерты",          emoji: "🧁", from: "#ffe6f1", to: "#f7aecf", ink: "#9b3466"},
  DAIRY:             {label: "Молочное",         emoji: "🧀", from: "#fff8d4", to: "#ffe27a", ink: "#8a6a00"},
  FRUITS_VEGETABLES: {label: "Фрукты и овощи",   emoji: "🥦", from: "#e5f8da", to: "#a6e391", ink: "#2f7a2c"},
  MEAT_FISH:         {label: "Мясо и рыба",      emoji: "🍣", from: "#ffe5e0", to: "#ffab9c", ink: "#a23b2b"},
  GROCERIES:         {label: "Бакалея",          emoji: "🛒", from: "#e8edff", to: "#b9c7ff", ink: "#3b4fa8"},
  DRINKS:            {label: "Напитки",          emoji: "🥤", from: "#dff4ff", to: "#9fdcfb", ink: "#1f6a8f"},
  OTHER:             {label: "Другое",           emoji: "🥡", from: "#e8f6ec", to: "#bfe6cb", ink: "#2b7650"}
};

const TITLE_HINTS: Array<[RegExp, FoodCategory, string?]> = [
  [/пиц/, "READY_MEALS", "🍕"], [/суш|ролл/, "MEAT_FISH", "🍣"], [/бургер/, "READY_MEALS", "🍔"],
  [/салат/, "FRUITS_VEGETABLES", "🥗"], [/коф|латте|капуч/, "DRINKS", "☕"], [/сок|напит|чай/, "DRINKS"],
  [/круасс|хлеб|выпеч|булоч|багет|пирож/, "BAKERY"], [/торт|пирожн|десерт|маффин|кекс/, "DESSERTS"],
  [/сыр|молок|йогурт|творог/, "DAIRY"], [/фрукт|овощ|яблок|банан/, "FRUITS_VEGETABLES"]
];

/** Theme for a listing or order: explicit category first, then a guess from the title. */
export function foodTheme(title: string, category?: FoodCategory | null): Theme {
  const text = title.toLowerCase();
  const hint = TITLE_HINTS.find(([pattern]) => pattern.test(text));
  const base = CATEGORY_THEME[category ?? hint?.[1] ?? "OTHER"] ?? CATEGORY_THEME.OTHER;
  return hint?.[2] ? {...base, emoji: hint[2]} : base;
}

export const themeStyle = (theme: Theme): CSSProperties =>
  ({"--cover-from": theme.from, "--cover-to": theme.to, "--cover-ink": theme.ink} as CSSProperties);
