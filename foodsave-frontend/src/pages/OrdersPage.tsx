import { ArrowRight, CheckCircle2, Clock3, CreditCard, Leaf, PackageCheck, QrCode, ShoppingBag, Star, XCircle } from "lucide-react";
import type { Order, Profile } from "../types";
import { dt, money, time } from "../utils";

type Props = {
  profile: Profile;
  orders: Order[];
  paidIds: ReadonlySet<string>;
  payingId: string | null;
  onPay: (order:Order) => void;
  onQr: (order:Order) => void;
  onReview: (order:Order) => void;
  onCancel: (order:Order) => void;
  onDiscover: () => void;
  onImpact: () => void;
};

const statusText: Record<string, string> = {RESERVED:"Забронирован",COMPLETED:"Завершён",CANCELLED:"Отменён",EXPIRED:"Истёк",NO_SHOW:"Не забран"};
export function OrdersPage({profile,orders,paidIds,payingId,onPay,onQr,onReview,onCancel,onDiscover,onImpact}:Props) {
  const active=orders.filter(o=>o.status==="RESERVED");
  const completed=orders.filter(o=>o.status==="COMPLETED");
  return <section className="section container orders-page">
    <div className="page-head"><div><span className="kicker">Личный кабинет</span><h1>Мои заказы</h1><p>Вся история бронирований, оплата и QR-коды для получения.</p></div><button className="primary-btn" onClick={onDiscover}><ShoppingBag size={17}/> Найти еду <ArrowRight size={16}/></button></div>
    <div className="orders-summary"><div><PackageCheck size={21}/><b>{orders.length}</b><span>Всего заказов</span></div><div><Clock3 size={21}/><b>{active.length}</b><span>Ожидают получения</span></div><div><CheckCircle2 size={21}/><b>{completed.length}</b><span>Завершено</span></div><div><Leaf size={21}/><b>{profile.foodPoints??0}</b><span>FoodPoints</span></div></div>
    <div className="heading"><div><span className="kicker">История покупок</span><h2>Заказы</h2></div><span className="count">{orders.length}</span></div>
    {orders.length===0?<div className="empty-page"><div className="empty"><ShoppingBag size={28}/><h3>Заказов пока нет</h3><p>Выбери первое предложение — заказ появится здесь.</p><button className="primary-btn" onClick={onDiscover}>К предложениям <ArrowRight size={16}/></button></div></div>:<div className="orders-list">{orders.map(o=>{
      const paid=paidIds.has(o.id);
      const reserved=o.status==="RESERVED";
      return <article className="order-card" key={o.id}>
        <div className="order-card-symbol"><PackageCheck size={23}/></div>
        <div className="order-card-center"><div className="order-card-top"><h3>{o.listingTitle}</h3><span className={`status status-${o.status.toLowerCase()}`}>{statusText[o.status]??o.status}</span></div>
          <p>№ {o.id.slice(0,8)} · {o.quantity} шт. · {dt(o.createdAt)}</p>
          <div className="order-card-pickup"><Clock3 size={14}/> Получение: {dt(o.pickupStart)} — {time(o.pickupEnd)}</div>
          {reserved&&<div className={paid?"payment-label paid":"payment-label unpaid"}>{paid?<><CheckCircle2 size={15}/> Оплачено</>:<><CreditCard size={15}/> Ожидает оплаты</>}</div>}
          <div className="order-card-buttons">
            {reserved&&!paid&&<button className="primary-btn small-action" disabled={payingId===o.id} onClick={()=>onPay(o)}><CreditCard size={15}/>{payingId===o.id?"Обработка…":"Оплатить"}</button>}
            {reserved&&paid&&<button className="primary-btn small-action" onClick={()=>onQr(o)}><QrCode size={15}/> Показать QR</button>}
            {reserved&&!paid&&<button className="ghost-btn small-action" onClick={()=>onCancel(o)}><XCircle size={15}/> Отменить</button>}
            {o.status==="COMPLETED"&&<button className="ghost-btn small-action" onClick={()=>onReview(o)}><Star size={15}/> Оставить отзыв</button>}
          </div>
        </div>
        <div className="order-card-total"><span>Сумма</span><b>{money(o.totalPrice)}</b></div>
      </article>;
    })}</div>}
    <div className="impact-card orders-impact"><div><span className="kicker">Твой вклад</span><h2>Покупки, которые спасают еду.</h2><p>Посмотри, сколько килограммов еды и денег удалось сохранить.</p></div><button className="primary-btn" onClick={onImpact}><Leaf size={17}/> Статистика</button></div>
  </section>;
}
