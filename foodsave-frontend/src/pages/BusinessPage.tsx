import { Building2, CirclePlus, MapPin, Pencil, Plus, ScanLine, Star, Trash2, PackageCheck, CircleCheck } from "lucide-react";
import type { BusinessProfile, Listing } from "../types";
import { dt, money } from "../utils";

type Props={
  business: BusinessProfile|null;
  listings: Listing[];
  rating: number|null;
  reviewCount: number;
  onProfile: ()=>void;
  onCreate: ()=>void;
  onEdit: (listing:Listing)=>void;
  onCancel: (listing:Listing)=>void;
  onScanner: ()=>void;
};

export function BusinessPage({business,listings,rating,reviewCount,onProfile,onCreate,onEdit,onCancel,onScanner}:Props){
  const active=listings.filter(l=>l.status==="ACTIVE");
  return <section className="section container business-page">
    <div className="page-head business-page-head"><div><span className="kicker">FoodSave for Business</span><h1>{business?.businessName||"Кабинет бизнеса"}</h1><p>{business?.address?`📍 ${business.address}`:"Сначала создай профиль и укажи адрес, чтобы покупатели находили твои предложения."}</p></div><div className="page-head-actions"><button className="ghost-btn" onClick={onScanner}><ScanLine size={17}/> Сканировать QR</button><button className="primary-btn" onClick={onProfile}>{business?<Pencil size={17}/>:<Building2 size={17}/>} {business?"Редактировать профиль":"Создать профиль"}</button></div></div>
    {!business&&<div className="business-warning"><Building2 size={20}/><div><b>Профиль бизнеса не заполнен</b><p>Создай профиль, укажи адрес своего заведения. Пока профиля нет, публикация объявлений недоступна.</p></div><button className="primary-btn" onClick={onProfile}>Настроить</button></div>}
    <div className="business-metrics"><div><PackageCheck size={23}/><b>{listings.length}</b><span>Моих объявлений</span></div><div><CircleCheck size={23}/><b>{active.length}</b><span>Активных</span></div><div><Star size={23}/><b>{rating!=null?rating.toFixed(1):"—"}</b><span>Рейтинг · {reviewCount} отзывов</span></div></div>
    <div className="business-section-head"><div><span className="kicker">Управление ассортиментом</span><h2>Мои предложения</h2><p>Здесь отображаются только объявления, полученные через API текущего бизнеса.</p></div><button className="primary-btn" onClick={onCreate} disabled={!business}><Plus size={17}/> Новое предложение</button></div>
    {listings.length>0?<div className="business-listings">{listings.map(l=><article className="business-item" key={l.id}>
      <div className="business-item-avatar">🥡</div><div className="business-item-info"><h3>{l.title}</h3><p>{l.description||"Без описания"}</p><span><MapPin size={13}/> {business?.address??"Адрес не указан"}</span><span>Забрать: {dt(l.pickupStart)}</span></div>
      <div className="business-item-price"><del>{money(l.originalPrice)}</del><b>{money(l.foodSavePrice)}</b><small>Остаток: {l.quantity}</small></div><div className="business-item-actions"><span className={`status status-${l.status.toLowerCase()}`}>{l.status}</span><div><button className="icon-btn" title="Редактировать" onClick={()=>onEdit(l)}><Pencil size={16}/></button><button className="danger-icon" title="Отменить предложение" disabled={l.status!=="ACTIVE"} onClick={()=>onCancel(l)}><Trash2 size={16}/></button></div></div>
    </article>)}</div>:<div className="business-empty"><CirclePlus size={34}/><h3>Пока нет предложений</h3><p>Создай первое объявление — клиенты смогут найти его по своему городу.</p><button className="primary-btn" onClick={onCreate} disabled={!business}><Plus size={16}/> Добавить предложение</button></div>}
    <div className="business-bottom-help"><ScanLine size={20}/><div><b>Получение заказов</b><p>Когда клиент придёт за едой, отсканируй его QR-код, чтобы завершить заказ и начислить FoodPoints.</p></div><button className="ghost-btn" onClick={onScanner}>Открыть сканер</button></div>
  </section>;
}
