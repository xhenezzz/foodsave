import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import {
  Ban, BarChart3, Building2, CheckCircle2, Leaf, LockOpen, PackageCheck, RefreshCw, Search,
  ShieldCheck, ShoppingBag, Store, Trash2, Users, Wallet, X
} from "lucide-react";
import { api } from "../api";
import { CountUp } from "../components";
import type { AdminListing, AdminOrder, AdminStats, AdminUser, ListingStatus, OrderStatus, Role } from "../types";
import { cityName, dt, errText, money, stagger } from "../utils";
import "./AdminPage.css";

type Tab = "overview" | "users" | "listings" | "orders";
type Notify = (toast: {type: "success" | "error"; text: string}) => void;

const ROLE_LABEL: Record<Role, string> = {CUSTOMER: "Покупатель", BUSINESS: "Бизнес", ADMIN: "Администратор"};
const LISTING_LABEL: Record<string, string> = {ACTIVE: "Активно", SOLD_OUT: "Распродано", EXPIRED: "Истекло", CANCELED: "Отменено", CANCELLED: "Отменено"};
const ORDER_LABEL: Record<string, string> = {RESERVED: "Забронирован", COMPLETED: "Завершён", CANCELLED: "Отменён", EXPIRED: "Истёк", NO_SHOW: "Не забран"};
const PAYMENT_LABEL: Record<string, string> = {SUCCESS: "Оплачен", PENDING: "Ожидает", FAILED: "Ошибка"};
const ORDER_COLORS: Record<string, string> = {RESERVED: "#e0a92b", COMPLETED: "#25a36b", CANCELLED: "#9aa7a0", EXPIRED: "#c9b48f", NO_SHOW: "#d1604b"};
const decimal = (n: number, digits = 1) => new Intl.NumberFormat("ru-RU", {maximumFractionDigits: digits}).format(n);

export function AdminPage({adminId, onToast}: {adminId: string; onToast: Notify}) {
  const [tab, setTab] = useState<Tab>("overview");
  const tabs: Array<{id: Tab; label: string; icon: typeof Users}> = [
    {id: "overview", label: "Обзор", icon: BarChart3},
    {id: "users", label: "Пользователи", icon: Users},
    {id: "listings", label: "Объявления", icon: Store},
    {id: "orders", label: "Заказы", icon: ShoppingBag}
  ];
  return <section className="section container admin-page">
    <div className="admin-hero" data-reveal>
      <div className="admin-hero-text">
        <span className="kicker"><ShieldCheck size={14}/> FoodSave Admin</span>
        <h1>Панель <em>администратора</em></h1>
        <p>Состояние платформы, пользователи, модерация объявлений и все заказы — в одном месте.</p>
      </div>
      <div className="admin-hero-art" aria-hidden="true">
        <div className="admin-cube"><span/><span/><span/><span/><span/><span/></div>
        <div className="admin-ring"/>
      </div>
    </div>
    <div className="admin-tabs" role="tablist" aria-label="Разделы админ-панели">
      {tabs.map(({id, label, icon: Icon}) => <button key={id} role="tab" aria-selected={tab === id}
        className={tab === id ? "active" : ""} onClick={() => setTab(id)}><Icon size={16}/>{label}</button>)}
      <span className="admin-tab-glider" style={{"--tab": tabs.findIndex(t => t.id === tab)} as React.CSSProperties}/>
    </div>
    <div className="admin-panel" key={tab}>
      {tab === "overview" && <Overview/>}
      {tab === "users" && <UsersTab adminId={adminId} onToast={onToast}/>}
      {tab === "listings" && <ListingsTab onToast={onToast}/>}
      {tab === "orders" && <OrdersTab/>}
    </div>
  </section>;
}

/** Loads data through `load`, re-running whenever `deps` change. */
function useAdminData<T>(load: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tick, setTick] = useState(0);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(load, deps);
  useEffect(() => {
    let alive = true;
    setLoading(true); setError("");
    run().then(result => { if (alive) setData(result); })
      .catch(err => { if (alive) setError(errText(err, "Не удалось загрузить данные")); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [run, tick]);
  return {data, setData, loading, error, reload: () => setTick(t => t + 1)};
}

function StateBlock({loading, error, empty, onRetry}: {loading: boolean; error: string; empty: boolean; onRetry: () => void}) {
  if (error) return <div className="admin-state admin-error" role="alert"><b>Ошибка:</b> {error}<button className="ghost-btn small" onClick={onRetry}><RefreshCw size={14}/> Повторить</button></div>;
  if (loading) return <div className="admin-skeleton" aria-busy="true">{[0, 1, 2, 3].map(i => <span key={i} style={stagger(i)}/>)}</div>;
  if (empty) return <div className="admin-state">Ничего не найдено.</div>;
  return null;
}

// ------------------------------------------------------------------ overview

function Overview() {
  const {data, loading, error, reload} = useAdminData(() => api.admin.stats(), []);
  if (!data) return <StateBlock loading={loading} error={error} empty={false} onRetry={reload}/>;
  const kpis = [
    {label: "Пользователей", value: data.totalUsers, note: `${data.usersByRole.CUSTOMER ?? 0} покупателей · ${data.usersByRole.BUSINESS ?? 0} бизнесов`, icon: Users},
    {label: "Объявлений", value: data.totalListings, note: `${data.listingsByStatus.ACTIVE ?? 0} активных сейчас`, icon: Store},
    {label: "Заказов", value: data.totalOrders, note: `${data.ordersByStatus.COMPLETED ?? 0} завершено`, icon: PackageCheck},
    {label: "Оплачено", value: data.paidOrders, note: money(data.revenue), icon: Wallet},
    {label: "Спасено еды, кг", value: data.foodSavedKg, note: `≈ ${decimal(data.co2SavedKg, 1)} кг CO₂e`, icon: Leaf},
    {label: "Заблокировано", value: data.blockedUsers, note: "аккаунтов", icon: Ban}
  ];
  return <>
    <div className="admin-toolbar"><span className="muted">Данные из базы FoodSave</span><button className="ghost-btn small" onClick={reload} disabled={loading}><RefreshCw size={14} className={loading ? "spin" : ""}/> Обновить</button></div>
    <div className="admin-kpis">
      {kpis.map(({label, value, note, icon: Icon}, index) => <div className="admin-kpi fs-stagger" data-tilt style={stagger(index)} key={label}>
        <span className="admin-kpi-icon"><Icon size={20}/></span>
        <strong>{Number.isInteger(value) ? <CountUp value={value}/> : decimal(value, 2)}</strong>
        <span>{label}</span><small>{note}</small>
      </div>)}
    </div>
    <div className="admin-grid">
      <div className="admin-card" data-reveal><ActivityChart data={data.activity}/></div>
      <div className="admin-card" data-reveal><StatusDonut counts={data.ordersByStatus} total={data.totalOrders}/></div>
    </div>
    <div className="admin-card" data-reveal>
      <div className="admin-card-head"><div><h3>Лучшие заведения</h3><p className="muted">По числу завершённых заказов</p></div><Building2 size={22}/></div>
      {data.topBusinesses.length ? <div className="admin-top">{data.topBusinesses.map((b, i) => {
        const max = data.topBusinesses[0].completedOrders || 1;
        return <div className="admin-top-row fs-stagger" style={stagger(i)} key={`${b.businessName}-${i}`}>
          <span className="admin-rank">{i + 1}</span>
          <div><b>{b.businessName}</b><small>{b.city ? cityName(b.city) : "—"} · {money(b.revenue)}</small>
            <div className="admin-bar"><i style={{width: `${(b.completedOrders / max) * 100}%`}}/></div></div>
          <strong>{b.completedOrders}</strong>
        </div>;
      })}</div> : <p className="muted">Завершённых заказов пока нет.</p>}
    </div>
  </>;
}

function ActivityChart({data}: {data: AdminStats["activity"]}) {
  const width = 640, height = 220, pad = 30;
  const maxOrders = Math.max(1, ...data.map(d => d.orders));
  const maxRevenue = Math.max(1, ...data.map(d => d.revenue));
  const step = (width - pad * 2) / Math.max(1, data.length);
  const bar = Math.max(6, step * 0.56);
  const line = data.map((d, i) => `${i ? "L" : "M"}${(pad + step * i + step / 2).toFixed(1)},${(height - pad - (d.revenue / maxRevenue) * (height - pad * 2)).toFixed(1)}`).join(" ");
  const totalOrders = data.reduce((s, d) => s + d.orders, 0);
  return <>
    <div className="admin-card-head"><div><h3>Активность за {data.length} дней</h3><p className="muted">Столбцы — заказы, линия — выручка по успешным оплатам</p></div><b>{totalOrders} зак.</b></div>
    <svg className="admin-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Заказы и выручка по дням">
      <defs><linearGradient id="admin-bar" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#3cc488"/><stop offset="1" stopColor="#1d7a52"/></linearGradient></defs>
      {[0.25, 0.5, 0.75, 1].map(f => <line key={f} x1={pad} x2={width - pad} y1={height - pad - f * (height - pad * 2)} y2={height - pad - f * (height - pad * 2)} stroke="currentColor" opacity=".08"/>)}
      {data.map((d, i) => {
        const h = (d.orders / maxOrders) * (height - pad * 2);
        return <g key={d.date}>
          <rect className="admin-chart-bar" style={stagger(i)} x={pad + step * i + (step - bar) / 2} y={height - pad - h} width={bar} height={Math.max(h, 1.5)} rx="4" fill="url(#admin-bar)"><title>{`${new Date(`${d.date}T12:00:00`).toLocaleDateString("ru-RU")}: ${d.orders} заказов, ${money(d.revenue)}`}</title></rect>
          {(i % 2 === 0 || data.length <= 8) && <text x={pad + step * i + step / 2} y={height - 8} textAnchor="middle" className="admin-chart-label">{new Date(`${d.date}T12:00:00`).toLocaleDateString("ru-RU", {day: "2-digit", month: "2-digit"})}</text>}
        </g>;
      })}
      <path key={line} className="fs7-line" pathLength={1} d={line} fill="none" stroke="#e0a92b" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  </>;
}

function StatusDonut({counts, total}: {counts: Record<string, number>; total: number}) {
  const radius = 62, circumference = 2 * Math.PI * radius;
  let offset = 0;
  const segments = Object.entries(counts).filter(([, n]) => n > 0).map(([status, n]) => {
    const length = (n / Math.max(1, total)) * circumference;
    const segment = {status, n, length, offset};
    offset += length;
    return segment;
  });
  return <>
    <div className="admin-card-head"><div><h3>Статусы заказов</h3><p className="muted">Все заказы платформы</p></div></div>
    <div className="admin-donut-wrap">
      <svg className="admin-donut" viewBox="0 0 160 160" role="img" aria-label="Распределение заказов по статусам">
        <circle cx="80" cy="80" r={radius} fill="none" stroke="#edf2ee" strokeWidth="18"/>
        {segments.map((s, i) => <circle key={s.status} className="admin-donut-seg" style={stagger(i)} cx="80" cy="80" r={radius} fill="none"
          stroke={ORDER_COLORS[s.status] ?? "#8aa"} strokeWidth="18" strokeDasharray={`${s.length} ${circumference}`} strokeDashoffset={-s.offset} transform="rotate(-90 80 80)"/>)}
        <text x="80" y="78" textAnchor="middle" className="admin-donut-total">{total}</text>
        <text x="80" y="96" textAnchor="middle" className="admin-chart-label">заказов</text>
      </svg>
      <ul className="admin-legend">{Object.entries(counts).map(([status, n]) => <li key={status}><i style={{background: ORDER_COLORS[status] ?? "#8aa"}}/>{ORDER_LABEL[status] ?? status}<b>{n}</b></li>)}</ul>
    </div>
  </>;
}

// ------------------------------------------------------------------ users

function UsersTab({adminId, onToast}: {adminId: string; onToast: Notify}) {
  const [query, setQuery] = useState("");
  const [submitted, setSubmitted] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const {data, setData, loading, error, reload} = useAdminData(() => api.admin.users({query: submitted, role}), [submitted, role]);
  const [blocking, setBlocking] = useState<AdminUser | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  async function apply(user: AdminUser, blocked: boolean, reason?: string) {
    setBusyId(user.id);
    try {
      const updated = await api.admin.setBlocked(user.id, blocked, reason);
      setData(list => list?.map(u => u.id === updated.id ? updated : u) ?? null);
      setBlocking(null);
      onToast({type: "success", text: blocked ? `${updated.username} заблокирован` : `${updated.username} разблокирован`});
    } catch (err) {
      onToast({type: "error", text: errText(err, "Не удалось изменить статус")});
    } finally { setBusyId(null); }
  }

  return <>
    <form className="admin-filters" onSubmit={(e: FormEvent) => { e.preventDefault(); setSubmitted(query); }}>
      <label className="catalog-search admin-search"><Search size={18}/><input type="search" placeholder="Имя, email или телефон…" value={query} onChange={e => setQuery(e.target.value)}/></label>
      <select value={role} onChange={e => setRole(e.target.value as Role | "")} aria-label="Роль"><option value="">Все роли</option><option value="CUSTOMER">Покупатели</option><option value="BUSINESS">Бизнес</option><option value="ADMIN">Администраторы</option></select>
      <button className="primary-btn">Найти</button>
    </form>
    <StateBlock loading={loading && !data} error={error} empty={!!data && data.length === 0} onRetry={reload}/>
    {data && data.length > 0 && <div className="admin-table-wrap"><table className="admin-table">
      <thead><tr><th>Пользователь</th><th>Роль</th><th>Город</th><th>Заказы</th><th>Статус</th><th/></tr></thead>
      <tbody>{data.map((u, i) => <tr key={u.id} className={`fs-stagger ${u.blocked ? "is-blocked" : ""}`} style={stagger(i)}>
        <td><div className="admin-user"><span className="avatar">{u.username.charAt(0).toUpperCase()}</span><div><b>{u.username}{u.id === adminId && <em> (вы)</em>}</b><small>{u.email} · {u.phoneNumber}</small>{u.businessName && <small><Building2 size={11}/> {u.businessName}</small>}</div></div></td>
        <td><span className={`admin-pill role-${u.role.toLowerCase()}`}>{ROLE_LABEL[u.role]}</span></td>
        <td>{cityName(u.city)}</td>
        <td>{u.role === "CUSTOMER" ? <span title="Всего / завершено / не забрано">{u.totalOrders} / {u.completedOrders} / {u.noShowCount}</span> : "—"}</td>
        <td>{u.blocked ? <span className="admin-pill danger" title={u.blockedReason ?? undefined}><Ban size={12}/> Заблокирован</span>
          : u.emailVerified ? <span className="admin-pill ok"><CheckCircle2 size={12}/> Активен</span> : <span className="admin-pill warn">Почта не подтверждена</span>}</td>
        <td className="admin-actions">{u.role !== "ADMIN" && (u.blocked
          ? <button className="ghost-btn small" disabled={busyId === u.id} onClick={() => void apply(u, false)}><LockOpen size={14}/> Разблокировать</button>
          : <button className="danger-btn small" disabled={busyId === u.id} onClick={() => setBlocking(u)}><Ban size={14}/> Заблокировать</button>)}</td>
      </tr>)}</tbody>
    </table></div>}
    {data && data.length >= 200 && <p className="muted">Показаны первые 200 результатов — уточни поиск.</p>}
    {blocking && <BlockDialog user={blocking} busy={busyId === blocking.id} onCancel={() => setBlocking(null)} onConfirm={reason => void apply(blocking, true, reason)}/>}
  </>;
}

function BlockDialog({user, busy, onCancel, onConfirm}: {user: AdminUser; busy: boolean; onCancel: () => void; onConfirm: (reason: string) => void}) {
  const [reason, setReason] = useState("");
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onCancel(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onCancel]);
  return <div className="backdrop"><div className="modal" role="dialog" aria-modal="true" aria-label="Блокировка пользователя">
    <button type="button" className="close" onClick={onCancel} aria-label="Закрыть"><X size={18}/></button>
    <div className="modal-icon admin-danger-icon"><Ban size={22}/></div>
    <h2>Заблокировать {user.username}?</h2>
    <p className="muted">Пользователь сразу потеряет доступ: вход и все запросы с текущим токеном будут отклонены. Его объявления и заказы не удаляются.</p>
    <form className="form-stack" onSubmit={e => { e.preventDefault(); onConfirm(reason); }}>
      <label className="field"><span>Причина · необязательно, увидит пользователь при входе</span><textarea maxLength={255} value={reason} onChange={e => setReason(e.target.value)} placeholder="Например: нарушение правил размещения"/></label>
      <div className="form-end"><button type="button" className="ghost-btn" onClick={onCancel}>Отмена</button><button className="danger-btn" disabled={busy}><Ban size={16}/> {busy ? "Блокируем…" : "Заблокировать"}</button></div>
    </form>
  </div></div>;
}

// ------------------------------------------------------------------ listings

function ListingsTab({onToast}: {onToast: Notify}) {
  const [query, setQuery] = useState("");
  const [submitted, setSubmitted] = useState("");
  const [status, setStatus] = useState<ListingStatus | "">("ACTIVE");
  const {data, setData, loading, error, reload} = useAdminData(() => api.admin.listings({query: submitted, status}), [submitted, status]);
  const [busyId, setBusyId] = useState<string | null>(null);

  async function cancel(listing: AdminListing) {
    if (!window.confirm(`Снять с публикации «${listing.title}» (${listing.businessName})?`)) return;
    setBusyId(listing.id);
    try {
      const updated = await api.admin.cancelListing(listing.id);
      setData(list => list?.map(l => l.id === updated.id ? updated : l) ?? null);
      onToast({type: "success", text: "Объявление снято с публикации"});
    } catch (err) {
      onToast({type: "error", text: errText(err, "Не удалось отменить объявление")});
    } finally { setBusyId(null); }
  }

  return <>
    <form className="admin-filters" onSubmit={e => { e.preventDefault(); setSubmitted(query); }}>
      <label className="catalog-search admin-search"><Search size={18}/><input type="search" placeholder="Название или заведение…" value={query} onChange={e => setQuery(e.target.value)}/></label>
      <select value={status} onChange={e => setStatus(e.target.value as ListingStatus | "")} aria-label="Статус"><option value="">Все статусы</option><option value="ACTIVE">Активные</option><option value="SOLD_OUT">Распроданные</option><option value="EXPIRED">Истёкшие</option><option value="CANCELED">Отменённые</option></select>
      <button className="primary-btn">Найти</button>
    </form>
    <StateBlock loading={loading && !data} error={error} empty={!!data && data.length === 0} onRetry={reload}/>
    {data && data.length > 0 && <div className="admin-cards">{data.map((l, i) => {
      const discount = l.originalPrice > 0 ? Math.round((1 - l.foodSavePrice / l.originalPrice) * 100) : 0;
      const closable = l.status === "ACTIVE" || l.status === "SOLD_OUT";
      return <article className="admin-listing fs-stagger" data-tilt style={stagger(i)} key={l.id}>
        <div className="admin-listing-top"><span className={`admin-pill status-${l.status.toLowerCase()}`}>{LISTING_LABEL[l.status] ?? l.status}</span><b className="admin-discount">−{discount}%</b></div>
        <h3>{l.title}</h3>
        <p><Building2 size={13}/> {l.businessName} · {l.city ? cityName(l.city) : "город не указан"}</p>
        <small>{l.ownerEmail}</small>
        <div className="admin-listing-meta"><span><del>{money(l.originalPrice)}</del> <b>{money(l.foodSavePrice)}</b></span><span>Остаток: {l.quantity}</span></div>
        <small>Выдача: {dt(l.pickupStart)} — {dt(l.pickupEnd)}</small>
        {closable && <button className="danger-btn small full" disabled={busyId === l.id} onClick={() => void cancel(l)}><Trash2 size={14}/> {busyId === l.id ? "Снимаем…" : "Снять с публикации"}</button>}
      </article>;
    })}</div>}
  </>;
}

// ------------------------------------------------------------------ orders

function OrdersTab() {
  const [status, setStatus] = useState<OrderStatus | "">("");
  const {data, loading, error, reload} = useAdminData(() => api.admin.orders(status), [status]);
  const totals = useMemo(() => data ? data.reduce((s, o) => s + Number(o.totalPrice), 0) : 0, [data]);
  return <>
    <div className="admin-filters">
      <select value={status} onChange={e => setStatus(e.target.value as OrderStatus | "")} aria-label="Статус заказа"><option value="">Все заказы</option>{Object.entries(ORDER_LABEL).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
      <button className="ghost-btn" onClick={reload} disabled={loading}><RefreshCw size={15} className={loading ? "spin" : ""}/> Обновить</button>
      {data && <span className="muted admin-filter-note">{data.length} заказов на сумму {money(totals)}</span>}
    </div>
    <StateBlock loading={loading && !data} error={error} empty={!!data && data.length === 0} onRetry={reload}/>
    {data && data.length > 0 && <div className="admin-table-wrap"><table className="admin-table">
      <thead><tr><th>Заказ</th><th>Покупатель</th><th>Заведение</th><th>Сумма</th><th>Статус</th><th>Оплата</th></tr></thead>
      <tbody>{data.map((o, i) => <tr key={o.id} className="fs-stagger" style={stagger(i)}>
        <td><b>{o.listingTitle}</b><small>№ {o.id.slice(0, 8)} · {o.quantity} шт. · {dt(o.createdAt)}</small></td>
        <td>{o.customerUsername}<small>{o.customerEmail}</small></td>
        <td>{o.businessName}</td>
        <td><b>{money(o.totalPrice)}</b></td>
        <td><span className={`admin-pill status-${o.status.toLowerCase()}`}>{ORDER_LABEL[o.status] ?? o.status}</span></td>
        <td>{o.paymentStatus ? <span className={`admin-pill pay-${o.paymentStatus.toLowerCase()}`}>{PAYMENT_LABEL[o.paymentStatus] ?? o.paymentStatus}</span> : <span className="muted">—</span>}</td>
      </tr>)}</tbody>
    </table></div>}
  </>;
}
