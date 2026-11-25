import { BusinessItemCover } from "./BusinessItemCover";
import { BasketArt, HowItWorks, PhoneQrArt, StoreArt } from "../visuals";
import { BusinessStatistics } from "./BusinessStatistics";
import { Building2, CircleCheck, Clock3, MapPin, PackageCheck, Pencil, Plus, ScanLine, ShieldAlert, Star, Trash2 } from "lucide-react";
import type { BusinessProfile, Listing } from "../types";
import { dt, money, stagger } from "../utils";
import { CountUp } from "../components";

type Props = {
  business: BusinessProfile | null;
  listings: Listing[];
  rating: number | null;
  reviewCount: number;
  onProfile: () => void;
  onCreate: () => void;
  onEdit: (listing: Listing) => void;
  onCancel: (listing: Listing) => void;
  onScanner: () => void;
};

const LISTING_STATUS: Record<string, string> = {ACTIVE: "Активно", SOLD_OUT: "Распродано", EXPIRED: "Истекло", CANCELED: "Отменено", CANCELLED: "Отменено"};

export function BusinessPage({business, listings, rating, reviewCount, onProfile, onCreate, onEdit, onCancel, onScanner}: Props) {
  const approved = business?.status === "APPROVED";
  const active = listings.filter(l => l.status === "ACTIVE" && l.moderationStatus !== "PENDING" && l.moderationStatus !== "REJECTED");
  const profileLabel = !business ? "Подать заявку" : approved ? "Редактировать профиль"
    : business.status === "REJECTED" ? "Исправить заявку" : "Изменить заявку";

  return <section className="section container business-page">
    <div className="page-head business-page-head business-hero"><StoreArt className="business-hero-art"/>
      <div><span className="kicker">FoodSave for Business</span><h1>{business?.businessName || "Кабинет бизнеса"}</h1>
        <p>{business?.address ? `📍 ${business.address}` : "Подай заявку на подключение — после проверки администратором ты сможешь публиковать предложения."}</p></div>
      <div className="page-head-actions">
        {approved && <button className="ghost-btn" onClick={onScanner}><ScanLine size={17}/> Сканировать QR</button>}
        <button className="primary-btn" onClick={onProfile}>{business ? <Pencil size={17}/> : <Building2 size={17}/>} {profileLabel}</button>
      </div>
    </div>

    {!approved && <ApplicationStatus business={business} onProfile={onProfile}/>}

    {approved && <>
      <div className="business-metrics">
        <div className="fs-stagger" data-tilt style={stagger(0)}><PackageCheck size={23}/><b><CountUp value={listings.length}/></b><span>Моих объявлений</span></div>
        <div className="fs-stagger" data-tilt style={stagger(1)}><CircleCheck size={23}/><b><CountUp value={active.length}/></b><span>Активных в каталоге</span></div>
        <div className="fs-stagger" data-tilt style={stagger(2)}><Star size={23}/><b>{rating != null ? rating.toFixed(1) : "—"}</b><span>Рейтинг · {reviewCount} отзывов</span></div>
      </div>
      <BusinessStatistics/>
      <div className="business-section-head"><div><span className="kicker">Управление ассортиментом</span><h2>Мои предложения</h2><p>Новые предложения появляются в каталоге после проверки модератором.</p></div><button className="primary-btn" onClick={onCreate}><Plus size={17}/> Новое предложение</button></div>
      {listings.length > 0 ? <div className="business-listings">{listings.map((l, index) => <article className="business-item fs-stagger" style={stagger(index)} key={l.id}>
        <BusinessItemCover listingId={l.id} title={l.title} category={l.category}/>
        <div className="business-item-info"><h3>{l.title}</h3><p>{l.description || "Без описания"}</p><span><MapPin size={13}/> {business?.address ?? "Адрес не указан"}</span><span>Забрать: {dt(l.pickupStart)}</span>
          {l.moderationStatus === "REJECTED" && l.moderationReason && <span className="moderation-note"><ShieldAlert size={13}/> {l.moderationReason} — исправь и сохрани, чтобы отправить снова</span>}
        </div>
        <div className="business-item-price"><del>{money(l.originalPrice)}</del><b>{money(l.foodSavePrice)}</b><small>Остаток: {l.quantity}</small></div>
        <div className="business-item-actions">
          <div className="business-item-badges">
            {l.moderationStatus === "PENDING" && <span className="status moderation-pending"><Clock3 size={11}/> На модерации</span>}
            {l.moderationStatus === "REJECTED" && <span className="status moderation-rejected">Отклонено</span>}
            <span className={`status status-${l.status.toLowerCase()}`}>{LISTING_STATUS[l.status] ?? l.status}</span>
          </div>
          <div><button className="icon-btn" title="Редактировать" onClick={() => onEdit(l)}><Pencil size={16}/></button><button className="danger-icon" title="Отменить предложение" disabled={l.status !== "ACTIVE"} onClick={() => onCancel(l)}><Trash2 size={16}/></button></div>
        </div>
      </article>)}</div>
        : <div className="business-empty" data-reveal><BasketArt className="empty-art"/><h3>Пока нет предложений</h3><p>Создай первое объявление — после проверки его увидят покупатели твоего города.</p><button className="primary-btn" onClick={onCreate}><Plus size={16}/> Добавить предложение</button></div>}
      <div className="business-bottom-help" data-reveal><PhoneQrArt className="help-art"/><div><b>Получение заказов</b><p>Когда клиент придёт за едой, отсканируй его QR-код, чтобы завершить заказ и начислить FoodPoints.</p></div><button className="ghost-btn" onClick={onScanner}>Открыть сканер</button></div>
    </>}

    {(!approved || listings.length === 0) && <HowItWorks audience="business"/>}
  </section>;
}

/** What a business sees until its application is approved. */
function ApplicationStatus({business, onProfile}: {business: BusinessProfile | null; onProfile: () => void}) {
  if (!business) {
    return <div className="application-card new" data-reveal>
      <span className="application-icon"><Building2 size={22}/></span>
      <div><b>Подключите заведение к FoodSave</b><p>Заполните заявку: название, тип, адрес с точкой на карте и контакты. После одобрения администратором можно публиковать предложения.</p></div>
      <button className="primary-btn" onClick={onProfile}>Подать заявку</button>
    </div>;
  }
  if (business.status === "REJECTED") {
    return <div className="application-card rejected" data-reveal>
      <span className="application-icon"><ShieldAlert size={22}/></span>
      <div><b>Заявка отклонена</b><p>{business.rejectionReason || "Администратор не указал причину."}</p><small>Исправь данные и отправь заявку снова.</small></div>
      <button className="primary-btn" onClick={onProfile}>Исправить заявку</button>
    </div>;
  }
  return <div className="application-card pending" data-reveal>
    <span className="application-icon"><Clock3 size={22}/></span>
    <div><b>Заявка на проверке</b><p>Администратор проверит данные заведения. Пока заявка не одобрена, публикация предложений недоступна.</p>
      {business.submittedAt && <small>Отправлена {dt(business.submittedAt)}</small>}</div>
    <button className="ghost-btn" onClick={onProfile}>Изменить заявку</button>
  </div>;
}
