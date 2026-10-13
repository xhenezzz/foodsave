import { ArrowRight, LocateFixed, MapPin, RefreshCw, ShoppingBag, Leaf, Clock3, Sparkles, SlidersHorizontal } from "lucide-react";
import { useMemo, useState } from "react";
import type { City, Listing } from "../types";
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
  const location = nearby ? coords : null;
  const effectiveSort = sort === "distance" && !location ? "recommended" : sort;
  const sorted = useMemo(() => sortListings(listings, effectiveSort, location), [listings, effectiveSort, location]);
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
      <div className="hero-art"><div className="hero-orb"/><div className="hero-food">🥑</div><div className="floating f1"><Leaf size={16}/><b>−90%</b><small>до 90% дешевле</small></div><div className="floating f2"><MapPin size={16}/><b>{cityName(city)}</b><small>только твой город</small></div></div>
    </section>
    <section className="impact-strip"><div className="container stat-grid discover-stats"><div className="stat"><b>{listings.length}</b><span>доступных предложений</span></div><div className="stat"><b>{cityName(city)}</b><span>выбранный город</span></div><div className="stat"><b>QR</b><span>подтверждение получения</span></div><div className="stat"><b>FoodPoints</b><span>баллы за спасённую еду</span></div></div></section>
    <section id="offers" className="section container page-offers">
      <div className="heading"><div><span className="kicker">{nearby?"В радиусе 10 км":cityName(city)}</span><h2>Спасти сегодня</h2></div><div className="heading-actions"><button className="ghost-btn small" onClick={nearby?onShowAll:onLocate}><MapPin size={15}/>{nearby?"Весь мой город":"Показать рядом"}</button><button className="ghost-btn small" onClick={onOrders}><ShoppingBag size={15}/> Мои заказы</button></div></div>
      <div className="city-notice"><MapPin size={17}/><span>Отбираем объявления для города <b>{cityName(city)}</b> из твоего профиля. Предложения с неизвестным или противоречивым адресом скрываются.</span></div>
      {hiddenUnknown > 0 && <div className="data-notice"><RefreshCw size={16}/><span>{hiddenUnknown} предложений скрыто: backend не вернул город или пригодные координаты. Чтобы не показывать чужой город, мы не угадываем расположение.</span></div>}
      <div className="recommendation-toolbar" aria-label="Сортировка предложений">
        <div className="recommendation-intro"><span className="recommendation-icon"><Sparkles size={19}/></span><div><b>Подбор для тебя</b><small>{location?"Учитываем скидку, рейтинг, количество и расстояние":"Учитываем скидку, рейтинг и наличие. Расстояние — после включения геолокации."}</small>{scoreSource&&<small className="score-source">{scoreSource}</small>}</div></div>
        <label className="recommendation-select"><SlidersHorizontal size={16}/><span>Сортировка</span><select value={effectiveSort} onChange={event => setSort(event.target.value as ListingSort)}><option value="recommended">Рекомендуем</option><option value="discount">Максимальная скидка</option><option value="price">Дешевле</option>{location && <option value="distance">Ближе ко мне</option>}</select></label>
      </div>
      {nearby&&coords&&<MapView user={coords} listings={sorted} recommended={effectiveSort==="recommended"}/>}
      {sorted.length>0?<div className="listing-grid">{sorted.map((listing,index)=><ListingCard key={listing.id} listing={listing} recommendationReason={effectiveSort==="recommended"&&index<3?recommendationReason(listing,location):undefined} onOpen={()=>onOpen(listing)}/>)}</div>:<div className="empty-page"><Empty/><p>Попробуй выбрать поиск рядом или зайди чуть позже.</p></div>}
      <div className="help-strip"><Clock3 size={19}/><div><b>Как работает FoodSave?</b><span>Выбери продукт → забронируй → оплати тестовым платежом → покажи QR бизнесу при получении.</span></div></div>
    </section>
  </>;
}
