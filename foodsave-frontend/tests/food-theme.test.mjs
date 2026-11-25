import test from "node:test";
import assert from "node:assert/strict";
import { CATEGORY_THEME, foodTheme, themeStyle } from "../src/foodTheme.ts";

test("у каждой категории есть подпись, emoji и цвета", () => {
  for (const [key, theme] of Object.entries(CATEGORY_THEME)) {
    assert.ok(theme.label && theme.emoji, key);
    assert.match(theme.from, /^#[0-9a-f]{6}$/i);
    assert.match(theme.to, /^#[0-9a-f]{6}$/i);
  }
});

test("явная категория объявления важнее догадки по названию", () => {
  assert.equal(foodTheme("Набор выпечки", "DRINKS").label, CATEGORY_THEME.DRINKS.label);
});

test("без категории тема угадывается по названию, иначе — «Другое»", () => {
  assert.equal(foodTheme("Круассаны с шоколадом").label, "Выпечка");
  assert.equal(foodTheme("Пицца маргарита").emoji, "🍕");
  assert.equal(foodTheme("Капучино и маффин").emoji, "☕");
  assert.equal(foodTheme("Сюрприз-бокс", null).label, "Другое");
});

test("тема превращается в CSS-переменные обложки", () => {
  const style = themeStyle(CATEGORY_THEME.BAKERY);
  assert.equal(style["--cover-from"], CATEGORY_THEME.BAKERY.from);
  assert.equal(style["--cover-ink"], CATEGORY_THEME.BAKERY.ink);
});
