import { useEffect, useId, useState } from "react";
import { BarChart3, Leaf, ReceiptText, RefreshCw, ShoppingBasket, TrendingUp, Wallet } from "lucide-react";
import { api } from "../api";
import type { BusinessDashboard } from "../types";
import { money, stagger } from "../utils";
import "./BusinessStatistics.css";

const decimal = (n: number, digits = 1) => new Intl.NumberFormat("ru-RU", {maximumFractionDigits: digits}).format(n);
const dayLabel = (date: string) => new Date(`${date}T12:00:00`).toLocaleDateString("ru-RU", {day:"2-digit", month:"2-digit"});

function RevenueChart({data}: {data: BusinessDashboard["revenueByDay"]}) {
  const id = useId();
  const width = 760, height = 218, padX = 48, padY = 23;
  const innerW = width - padX * 2, innerH = height - padY * 2;
  const max = Math.max(1, ...data.map(d => d.revenue));
  const steps = Math.max(1, data.length - 1);
  const coords = data.map((d,i) => ({x:padX + (i / steps)*innerW, y:height-padY-(d.revenue/max)*innerH}));
  const line = coords.map((p,i)=>`${i === 0 ? "M" : "L"}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(" ");
  const area = coords.length ? `${line} L${coords.at(-1)!.x},${height-padY} L${coords[0].x},${height-padY} Z` : "";
  const labels = [...new Set([0, Math.floor((data.length-1)/4), Math.floor((data.length-1)/2), Math.floor((data.length-1)*.75), data.length-1])];
  if (!data.length) return <p className="fs7-muted">Нет данных по дням.</p>;
  return <div className="fs7-chart-scroll"><svg className="fs7-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="График успешных оплат по дням">
    <defs><linearGradient id={id} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#25b579" stopOpacity=".30"/><stop offset="100%" stopColor="#25b579" stopOpacity="0"/></linearGradient></defs>
    {[0,.25,.5,.75,1].map((f,i)=><g key={i}><line x1={padX} x2={width-padX} y1={height-padY-f*innerH} y2={height-padY-f*innerH} stroke="currentColor" opacity=".12"/><text x={padX-8} y={height-padY-f*innerH+4} textAnchor="end" className="fs7-chart-label">{decimal(max*f,0)}</text></g>)}
    {/* Keyed by the path so the line re-draws when the period or data changes. */}
    <path key={`a${area}`} className="fs7-area" d={area} fill={`url(#${id})`}/><path key={`l${line}`} className="fs7-line" pathLength={1} d={line} fill="none" stroke="#18a66d" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round"/>
    {coords.map((p,i) => data[i].revenue > 0 && data.length <= 30 ? <circle key={`${line}-${i}`} style={stagger(i)} cx={p.x} cy={p.y} r="3" fill="#18a66d"><title>{`${dayLabel(data[i].date)}: ${money(data[i].revenue)}`}</title></circle>:null)}
    {labels.map(i=><text key={i} x={coords[i].x} y={height-4} textAnchor="middle" className="fs7-chart-label">{dayLabel(data[i].date)}</text>)}
  </svg></div>;
}

export function BusinessStatistics() {
  const [days,setDays] = useState<7|30|90>(30);
  const [refresh,setRefresh] = useState(0);
  const [data,setData] = useState<BusinessDashboard|null>(null);
  const [loading,setLoading] = useState(true);
  const [error,setError] = useState("");
  useEffect(()=>{
    let active=true;
    setLoading(true); setError("");
    api.getBusinessStatistics(days).then(result=>{
      if(active) setData(result);
    }).catch(err=>{
      if(active){setData(null);setError(err instanceof Error ? err.message : "Ошибка загрузки статистики");}
    }).finally(()=>{if(active)setLoading(false);});
    return ()=>{active=false;};
  },[days,refresh]);
  const cards = data ? [
    {name:"Демо-выручка",value:money(data.revenue),note:"Тестовые успешные оплаты",icon:Wallet},
    {name:"Оплачено заказов",value:decimal(data.paidOrders,0),note:"За выбранный период",icon:ReceiptText},
    {name:"Получено заказов",value:decimal(data.completedOrders,0),note:"COMPLETED",icon:ShoppingBasket},
    {name:"Средний чек",value:money(data.averageCheck),note:"Тестовые успешные оплаты",icon:TrendingUp},
    {name:"Спасено еды",value:`${decimal(data.foodSavedKg,2)} кг`,note:"Полученные заказы",icon:Leaf},
    {name:"Спасено от оплаченного",value:`${decimal(data.rescuedFoodPercent,1)}%`,note:"По известному весу",icon:BarChart3}
  ] : [];
  return <section className="fs7-panel" aria-label="Аналитика бизнеса">
    <header className="fs7-heading"><div><span className="kicker">Аналитика FoodSave</span><h2>Статистика бизнеса</h2><p className="fs7-muted">Данные заказов и тестовых оплат (реальных списаний нет)</p></div><div className="fs7-actions"><div className="fs7-tabs" aria-label="Период">{([7,30,90] as const).map(d=><button key={d} className={days===d?"selected":""} onClick={()=>setDays(d)}>{d} дней</button>)}</div><button className="fs7-reload" onClick={()=>setRefresh(v=>v+1)} disabled={loading} title="Обновить статистику"><RefreshCw size={17}/></button></div></header>
    {error&&<div className="fs7-error" role="alert"><b>Статистика недоступна:</b> {error} <button onClick={()=>setRefresh(v=>v+1)}>Повторить</button></div>}
    {loading&&!data&&<p className="fs7-muted">Загружаем аналитику…</p>}
    {data&&<>
      {loading&&<p className="fs7-muted" role="status">Обновляем…</p>}
      <div className="fs7-kpis">{cards.map((c,index)=><div className="fs7-kpi fs-stagger" data-tilt style={stagger(index)} key={c.name}><div className="fs7-kpi-title"><span>{c.name}</span><c.icon size={19}/></div><strong>{c.value}</strong><small>{c.note}</small></div>)}</div>
      <div className="fs7-card" data-reveal><div className="fs7-card-head"><div><h3>Тестовая выручка по дням</h3><p className="fs7-muted">Дата успешной оплаты · ₸</p></div><b>{money(data.revenue)}</b></div><RevenueChart data={data.revenueByDay}/></div>
      <div className="fs7-two"><div className="fs7-card" data-reveal><div className="fs7-card-head"><div><h3>Топ-5 продуктов</h3><p className="fs7-muted">По количеству полученных единиц</p></div></div>{data.topProducts.length?<div className="fs7-products">{data.topProducts.map((p,i)=><div className="fs7-product" key={p.listingId}><span className="fs7-rank">{i+1}</span><div><b>{p.title}</b><small>{p.completedOrders} заказов · {money(p.revenue)} по заказам</small></div><strong>{p.quantitySold} шт.</strong></div>)}</div>:<p className="fs7-muted">За этот период нет завершённых заказов.</p>}</div>
      <div className="fs7-card" data-reveal><div className="fs7-card-head"><div><h3>Экологический эффект</h3><p className="fs7-muted">Приблизительная оценка</p></div><Leaf size={24}/></div><div className="fs7-big">{decimal(data.foodSavedKg,2)} <span>кг еды</span></div><div className="fs7-progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={data.rescuedFoodPercent}><div style={{width:`${Math.max(0,Math.min(100,data.rescuedFoodPercent))}%`}}/></div><p className="fs7-muted">{decimal(data.rescuedFoodPercent,1)}% от известной массы оплаченных заказов получили статус COMPLETED</p><div className="fs7-co2">≈ {decimal(data.estimatedCo2SavedKg,2)} кг CO₂e — оценка по коэффициенту 2,5</div></div></div>
      <p className="fs7-footnote">Период для завершённых заказов определяется датой создания заказа (в Order пока нет completedAt). Выручка — по дате оплаты и статусу SUCCESS; возвраты не моделируются. Показатели не являются чистой прибылью. Для еды учитываются только заказы с сохранённым весом.</p>
    </>}
  </section>;
}
