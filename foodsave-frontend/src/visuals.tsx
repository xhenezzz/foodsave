/**
 * FoodSave visuals: category themes and hand-drawn SVG illustrations.
 * Everything is inline SVG/CSS — no external images to load, license or break.
 */
import type { FoodCategory } from "./types";
import { foodTheme, themeStyle } from "./foodTheme";

export { CATEGORY_THEME, foodTheme, themeStyle } from "./foodTheme";

/** Colourful placeholder used when a listing has no photo. */
export function FoodCover({title, category, compact = false}: {title: string; category?: FoodCategory | null; compact?: boolean}) {
  const theme = foodTheme(title, category);
  return <div className={`food-cover${compact ? " compact" : ""}`} style={themeStyle(theme)} aria-hidden="true">
    <span className="food-cover-blob b1"/><span className="food-cover-blob b2"/>
    <span className="food-cover-emoji">{theme.emoji}</span>
  </div>;
}

// ------------------------------------------------------------------ illustrations

type ArtProps = {className?: string; title?: string};
const svgProps = (title: string | undefined, className = "") => ({
  className: `fs-art ${className}`.trim(),
  role: title ? "img" : undefined,
  "aria-hidden": title ? undefined : true,
  "aria-label": title
});

/** Paper bag full of rescued groceries. */
export function BasketArt({className, title}: ArtProps) {
  return <svg viewBox="0 0 240 200" {...svgProps(title, className)}>
    <ellipse cx="120" cy="184" rx="86" ry="10" fill="#1f5c3b" opacity=".12"/>
    <circle cx="120" cy="92" r="78" fill="#e6f6dd"/>
    <path d="M80 70 L96 22 L108 26 L96 72Z" fill="#f2c27b"/><path d="M84 66 l10-34" stroke="#d99a45" strokeWidth="3" strokeLinecap="round" strokeDasharray="5 6"/>
    <path d="M118 66 c-8-26 6-44 22-46 c-2 16 -6 30 -10 46Z" fill="#5cbf6a"/><path d="M128 66 c4-22 22-34 38-30 c-8 14 -18 24 -28 32Z" fill="#3fa35a"/>
    <circle cx="154" cy="62" r="17" fill="#ef5b4c"/><path d="M154 46 q4-8 10-8" stroke="#6b3e1d" strokeWidth="3" fill="none" strokeLinecap="round"/><ellipse cx="148" cy="56" rx="5" ry="3" fill="#fff" opacity=".45"/>
    <path d="M60 74 H180 L168 178 H72Z" fill="#d9a66a"/><path d="M60 74 H180 L177 96 H63Z" fill="#c78f52"/>
    <path d="M100 74 q20 18 40 0" stroke="#8c5b2a" strokeWidth="4" fill="none" strokeLinecap="round"/>
    <circle cx="120" cy="132" r="22" fill="#fff8ec"/><path d="M110 138 c0-14 10-22 22-22 c0 14-10 22-22 22Z" fill="#2e9b62"/><path d="M112 136 l14-14" stroke="#fff8ec" strokeWidth="2"/>
    <path d="M190 40 l3 8 8 3-8 3-3 8-3-8-8-3 8-3Z" fill="#ffc947"/><path d="M44 104 l2 5 5 2-5 2-2 5-2-5-5-2 5-2Z" fill="#ff9b5e"/>
  </svg>;
}

/** Shop front with an awning — partner businesses. */
export function StoreArt({className, title}: ArtProps) {
  return <svg viewBox="0 0 240 200" {...svgProps(title, className)}>
    <ellipse cx="120" cy="186" rx="96" ry="9" fill="#1f5c3b" opacity=".12"/>
    <circle cx="190" cy="44" r="20" fill="#ffd25e"/>
    <rect x="38" y="70" width="164" height="112" rx="8" fill="#fffaf2"/>
    <rect x="38" y="70" width="164" height="112" rx="8" fill="none" stroke="#e7dccb" strokeWidth="2"/>
    <path d="M30 52 H210 L202 82 H38Z" fill="#2e9b62"/>
    {[0, 1, 2, 3, 4, 5].map(i => <path key={i} d={`M${38 + i * 28} 82 a14 12 0 0 0 28 0`} fill={i % 2 ? "#2e9b62" : "#7fd39c"}/>)}
    {[0, 1, 2].map(i => <path key={i} d={`M${54 + i * 54} 52 L${48 + i * 54} 82`} stroke="#fff" strokeWidth="10" opacity=".9"/>)}
    <rect x="52" y="104" width="82" height="58" rx="6" fill="#dff2e6"/>
    <path d="M58 132 H128 M58 150 H128" stroke="#b9dcc6" strokeWidth="3"/>
    <text x="72" y="128" fontSize="20">🥐</text><text x="98" y="128" fontSize="20">🧁</text><text x="72" y="148" fontSize="16">🥖</text><text x="100" y="148" fontSize="16">🍎</text>
    <rect x="146" y="104" width="40" height="78" rx="5" fill="#ffb26b"/><circle cx="178" cy="146" r="3" fill="#8a4a12"/>
    <rect x="148" y="112" width="36" height="20" rx="4" fill="#fff4e3"/><text x="152" y="126" fontSize="10" fontWeight="800" fill="#c0581b">OPEN</text>
    <circle cx="120" cy="40" r="15" fill="#fff"/><path d="M112 45 c0-10 7-16 16-16 c0 10-7 16-16 16Z" fill="#2e9b62"/>
  </svg>;
}

/** Phone with an order QR and a confirmation tick. */
export function PhoneQrArt({className, title}: ArtProps) {
  const qr = [[0,0],[1,0],[2,0],[0,1],[2,1],[0,2],[1,2],[2,2],[4,0],[5,1],[4,2],[6,0],[6,2],[0,4],[1,5],[2,4],[0,6],[2,6],[4,4],[5,5],[6,4],[4,6],[6,6],[3,3],[5,3],[3,5]];
  return <svg viewBox="0 0 240 200" {...svgProps(title, className)}>
    <ellipse cx="120" cy="188" rx="70" ry="8" fill="#1f5c3b" opacity=".12"/>
    <circle cx="120" cy="96" r="80" fill="#e4f1ff"/>
    <rect x="76" y="20" width="88" height="164" rx="18" fill="#1d3b2c"/>
    <rect x="83" y="30" width="74" height="144" rx="12" fill="#fff"/>
    <rect x="106" y="34" width="28" height="5" rx="2.5" fill="#1d3b2c"/>
    <g transform="translate(92 58)">{qr.map(([x, y]) => <rect key={`${x}-${y}`} x={x * 8} y={y * 8} width="7" height="7" rx="1.5" fill="#1d3b2c"/>)}</g>
    <rect x="92" y="122" width="56" height="8" rx="4" fill="#dcefe2"/><rect x="100" y="136" width="40" height="8" rx="4" fill="#eef5f0"/>
    <circle cx="166" cy="140" r="24" fill="#2e9b62"/><path d="M155 140 l8 8 15-16" stroke="#fff" strokeWidth="6" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M52 58 l3 8 8 3-8 3-3 8-3-8-8-3 8-3Z" fill="#ffc947"/>
  </svg>;
}

/** Planet with a sprout — impact / CO₂. */
export function PlanetArt({className, title}: ArtProps) {
  return <svg viewBox="0 0 240 200" {...svgProps(title, className)}>
    <circle cx="120" cy="104" r="70" fill="#6cc4f0"/>
    <path d="M70 70 c18-6 26 6 40 4 c10-2 8 18 -6 22 c-12 4 -10 18 -26 18 c-12 0-20-14-18-26 c2-8 4-14 10-18Z" fill="#4fbf73"/>
    <path d="M140 120 c14-8 34-4 40 8 c-6 18-20 30-36 34 c-6-14-12-28-4-42Z" fill="#4fbf73"/>
    <path d="M138 52 c10 0 18 6 20 14 c-10 2-22-2-20-14Z" fill="#4fbf73"/>
    <circle cx="120" cy="104" r="70" fill="none" stroke="#fff" strokeWidth="4" opacity=".5"/>
    <path d="M120 34 V12" stroke="#2e7d4a" strokeWidth="5" strokeLinecap="round"/>
    <path d="M120 22 c-4-14-18-18-28-14 c4 12 16 18 28 14Z" fill="#3fa35a"/><path d="M120 18 c6-12 20-14 28-8 c-6 10-18 14-28 8Z" fill="#5cbf6a"/>
    <path d="M196 64 l3 8 8 3-8 3-3 8-3-8-8-3 8-3Z" fill="#ffc947"/><circle cx="44" cy="140" r="6" fill="#ff9b5e"/>
  </svg>;
}

/** Three-step explainer used on the landing and the catalog. */
export function HowItWorks({audience = "customer"}: {audience?: "customer" | "business"}) {
  const steps = audience === "business"
    ? [
      {art: <StoreArt/>, title: "Заполни профиль", text: "Название, адрес и точка на карте — покупатели твоего города увидят заведение."},
      {art: <BasketArt/>, title: "Опубликуй излишки", text: "Фото, цена со скидкой и окно выдачи. AI поможет с описанием по фото."},
      {art: <PhoneQrArt/>, title: "Выдай по QR", text: "Покупатель приходит с оплаченным заказом — сканируешь QR, и готово."}
    ]
    : [
      {art: <BasketArt/>, title: "Выбери еду", text: "Свежая выпечка, готовые блюда и продукты рядом со скидкой до 90%."},
      {art: <PhoneQrArt/>, title: "Забронируй и оплати", text: "Бронь держится 15 минут, оплата — онлайн через Stripe."},
      {art: <PlanetArt/>, title: "Забери и спаси", text: "Покажи QR в заведении. Каждый заказ — меньше отходов и +10 FoodPoints."}
    ];
  return <section className="how-it-works" data-reveal>
    <div className="how-head"><span className="kicker">Как это работает</span><h2>{audience === "business" ? "Три шага до первых продаж" : "Три шага, чтобы спасти еду"}</h2></div>
    <ol className="how-steps">
      {steps.map((step, i) => <li className="how-step" key={step.title}>
        <div className="how-art">{step.art}<span className="how-num">{i + 1}</span></div>
        <h3>{step.title}</h3><p>{step.text}</p>
      </li>)}
    </ol>
  </section>;
}
