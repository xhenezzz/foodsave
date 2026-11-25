import { ArrowRight, LocateFixed, MapPin, RefreshCw, ShoppingBag, Leaf, Sparkles, SlidersHorizontal, Search, X } from "lucide-react";
import { useMemo, useState } from "react";
import type { City, FoodCategory, Listing } from "../types";
import { CATEGORY_THEME, HowItWorks, themeStyle } from "../visuals";
import { backendRecommendationScore, recommendationReason, sortListings } from "../recommendations";
import type { ListingSort } from "../recommendations";
import { cityName } from "../utils";
import { Empty, ListingCard } from "../components";
import { MapView } from "../MapView";

type Props = {
  city: City;
  listings: Listing[];
  hiddenUnknown: number;
  nearby: boolean;
  coords: {lat:number; lon:number} | null;
  locating: boolean;
  onLocate: () => void;
  onShowAll: () => void;
  onOpen: (listing:Listing) => void;
  onOrders: () => void;
};

export function DiscoverPage({city,listings,hiddenUnknown,nearby,coords,locating,onLocate,onShowAll,onOpen,onOrders}:Props) {
  const [sort, setSort] = useState<ListingSort>("recommended");
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<FoodCategory | "ALL">("ALL");
  // Chips only for categories that actually have offers right now.
  const categories = useMemo(() => {
    const counts = new Map<FoodCategory, number>();
    for (const item of listings) {
      const key = item.category ?? "OTHER";
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
    return [...counts.entries()].sort((a, b) => b[1] - a[1]);
  }, [listings]);
  const activeCategory = category !== "ALL" && categories.some(([key]) => key === category) ? category : "ALL";
  const location = nearby ? coords : null;
  const effectiveSort = sort === "distance" && !location ? "recommended" : sort;
  const sorted = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase("ru-RU");
    const inCategory = activeCategory === "ALL" ? listings : listings.filter(item => (item.category ?? "OTHER") === activeCategory);
    const filtered = normalized ? inCategory.filter(item => [item.title, item.description, item.businessName].some(text => (text || "").toLocaleLowerCase("ru-RU").includes(normalized))) : inCategory;
    return sortListings(filtered, effectiveSort, location);
  }, [listings, effectiveSort, location, query, activeCategory]);
  const serverRated = listings.filter(item => backendRecommendationScore(item) !== null).length;
  const scoreSource = !listings.length ? "" : serverRated === listings.length
    ? "Рейтинг рассчитан на сервере"
    : serverRated > 0 ? "Рейтинг из API; для части товаров используется резервный расчёт"
    : "Рейтинг рассчитывается локально до обновления backend";
  return <>
    <section className="hero container discover-hero">
      <div>
        <span className="eyebrow"><Leaf size={15}/> FoodSave · {cityName(city)}</span>
        <h1>Хорошая еда.<br/><em>Не пропадёт.</em></h1>
        <p>Забирай свежие продукты с большими скидками, пока они не превратились в пищевые отходы.</p>
        <div className="hero-actions">
          <a className="primary-btn" href="#offers">Смотреть предложения <ArrowRight size={17}/></a>
          <button className="ghost-btn" onClick={onLocate} disabled={locating}><LocateFixed size={17}/>{locating?"Определяем адрес…":"Рядом со мной"}</button>
        </div>
      </div>
      <div className="hero-art" data-tilt><div className="hero-orb"/><div className="hero-food"><span>🥐</span></div><div className="floating f1"><Leaf size={16}/><b>−90%</b><small>выгодные предложения</small></div><div className="floating f2"><MapPin size={16}/><b>{cityName(city)}</b><small>только твой город</small></div></div>
    </section>
    <section className="impact-strip"><div className="container stat-grid discover-stats"><div className="stat"><b>{listings.length}</b><span>доступных предложений</span></div><div className="stat"><b>{cityName(city)}</b><span>выбранный город</span></div><div className="stat"><b>QR</b><span>подтверждение получения</span></div><div className="stat"><b>FoodPoints</b><span>баллы за спасённую еду</span></div></div></section>
    <section id="offers" className="section container page-offers">
      <div className="heading"><div><span className="kicker">{nearby?"В радиусе 10 км":cityName(city)}</span><h2>Спасти сегодня</h2></div><div className="heading-actions"><button className="ghost-btn small" onClick={nearby?onShowAll:onLocate}><MapPin size={15}/>{nearby?"Весь мой город":"Показать рядом"}</button><button className="ghost-btn small" onClick={onOrders}><ShoppingBag size={15}/> Мои заказы</button></div></div>
      <div className="city-notice"><MapPin size={17}/><span>Отбираем объявления для города <b>{cityName(city)}</b> из твоего профиля. Предложения с неизвестным или противоречивым адресом скрываются.</span></div>
      {hiddenUnknown > 0 && <div className="data-notice"><RefreshCw size={16}/><span>{hiddenUnknown} предложений скрыто: backend не вернул город или пригодные координаты. Чтобы не показывать чужой город, мы не угадываем расположение.</span></div>}
      <div className="catalog-search-row"><label className="catalog-search" htmlFor="listing-search"><Search size={19}/><input id="listing-search" type="search" placeholder="Найти еду, кафе или пекарню…" value={query} onChange={event=>setQuery(event.target.value)} autoComplete="off"/>{query&&<button type="button" className="search-clear" onClick={()=>setQuery("")} aria-label="Очистить поиск"><X size={17}/></button>}</label><span className="catalog-results">{sorted.length} {sorted.length===1?"предложение":"предложений"}</span></div>
      {categories.length > 1 && <div className="category-chips" role="group" aria-label="Категории">
        <button type="button" className={activeCategory === "ALL" ? "category-chip active" : "category-chip"} onClick={() => setCategory("ALL")}><i>✨</i>Все<b>{listings.length}</b></button>
        {categories.map(([key, count]) => { const theme = CATEGORY_THEME[key]; return <button type="button" key={key} style={themeStyle(theme)} className={activeCategory === key ? "category-chip active" : "category-chip"} onClick={() => setCategory(key)}><i>{theme.emoji}</i>{theme.label}<b>{count}</b></button>; })}
      </div>}
      <div className="recommendation-toolbar" data-reveal aria-label="Сортировка предложений">
        <div className="recommendation-intro"><span className="recommendation-icon"><Sparkles size={19}/></span><div><b>Подбор для тебя</b><small>{location?"Учитываем скидку, рейтинг, количество и расстояние":"Учитываем скидку, рейтинг и наличие. Расстояние — после включения геолокации."}</small>{scoreSource&&<small className="score-source">{scoreSource}</small>}</div></div>
        <label className="recommendation-select"><SlidersHorizontal size={16}/><span>Сортировка</span><select value={effectiveSort} onChange={event => setSort(event.target.value as ListingSort)}><option value="recommended">Рекомендуем</option><option value="discount">Максимальная скидка</option><option value="price">Дешевле</option>{location && <option value="distance">Ближе ко мне</option>}</select></label>
      </div>
      {nearby&&coords&&<MapView user={coords} listings={sorted} recommended={effectiveSort==="recommended"}/>}
      {sorted.length>0?<div className="listing-grid">{sorted.map((listing,index)=><ListingCard key={listing.id} index={index} listing={listing} recommendationReason={effectiveSort==="recommended"&&index<3?recommendationReason(listing,location):undefined} onOpen={()=>onOpen(listing)}/>)}</div>:<div className="empty-page"><Empty/><p>{query ? "По этому запросу ничего не найдено. Попробуй другое название." : "Пока предложений нет. Попробуй выбрать поиск рядом или зайди чуть позже."}</p>{query&&<button className="ghost-btn" onClick={()=>setQuery("")}>Сбросить поиск</button>}</div>}
      <HowItWorks/>
    </section>
  </>;
}
