import { AlertTriangle, Award, CheckCircle2, Clock3, ShieldCheck, Star, X } from "lucide-react";
import type { Loyalty, ReputationLevel } from "../types";

const LEVELS: Record<ReputationLevel, string> = {
  NEW: "Новичок", BRONZE: "Бронза", SILVER: "Серебро", GOLD: "Золото", NEEDS_IMPROVEMENT: "Нужно улучшить"
};

export function ReputationModal({data, loading, error, onRetry, onClose}: {
  data: Loyalty | null; loading: boolean; error: string | null; onRetry: () => void; onClose: () => void;
}) {
  return <div className="backdrop" role="presentation">
    <section className="modal reputation-modal" role="dialog" aria-modal="true" aria-label="Моя репутация">
      <button type="button" className="close" aria-label="Закрыть" onClick={onClose}><X size={18}/></button>
      <div className="reputation-heading"><div className="reputation-icon"><Award size={24}/></div>
        <span className="kicker">FoodSave Loyalty</span><h2>Моя репутация</h2>
        <p>Получай еду вовремя, зарабатывай FoodPoints и поддерживай высокий рейтинг надёжности.</p>
      </div>
      {loading && <p className="reputation-message">Загружаем репутацию…</p>}
      {!loading && error && <div className="reputation-error"><AlertTriangle size={18}/><span>{error}</span><button className="ghost-btn small" type="button" onClick={onRetry}>Повторить</button></div>}
      {!loading && data && <>
        <div className="reputation-level"><Star size={19}/><span>Уровень: <b>{LEVELS[data.reputationLevel]}</b></span></div>
        <div className="reputation-stats">
          <div><b>{data.foodPoints}</b><span>FoodPoints</span></div>
          <div><b>{data.reliabilityPercent === null ? "—" : `${data.reliabilityPercent}%`}</b><span>Надёжность</span></div>
          <div><b>{data.completedOrders}</b><span>Получено заказов</span></div>
          <div><b>{data.noShowCount}</b><span>NO_SHOW</span></div>
        </div>
        {!data.canCreateOrder ? <div className="reputation-blocked"><AlertTriangle size={20}/>
          <div><b>Бронирования временно ограничены</b><p>Можно просматривать предложения, но создавать новые заказы нельзя до {data.bookingBlockedUntil ? new Date(data.bookingBlockedUntil).toLocaleString("ru-RU",{day:"numeric",month:"long",year:"numeric",hour:"2-digit",minute:"2-digit"}) : "окончания блокировки"}.</p></div>
        </div> : <div className="reputation-allowed"><CheckCircle2 size={19}/><span>Бронирования доступны</span></div>}
        <div className="reputation-rules">
          <h3><ShieldCheck size={18}/> Как работают баллы?</h3>
          <p><b>+10</b> FoodPoints за полученный заказ.</p>
          <p><b>−5</b> FoodPoints за оплаченный, но не полученный заказ (NO_SHOW).</p>
          <p><Clock3 size={14}/> Каждое третье нарушение NO_SHOW — ограничение новых бронирований на 7 дней.</p>
          <small>До следующего порога: {data.noShowsUntilNextBlock} NO_SHOW. Надёжность рассчитана по завершённым заказам и NO_SHOW; без истории не оценивается.</small>
        </div>
      </>}
    </section>
  </div>;
}
