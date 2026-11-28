import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { ArrowLeft, ArrowRight, CheckCircle2, ClipboardCheck, MapPin, Send, ShieldCheck, Store } from "lucide-react";
import { BusinessLocationPicker, type BusinessLocation } from "../BusinessLocationPicker";
import { api } from "../api";
import type { BusinessApplication, BusinessProfile, BusinessType, City } from "../types";
import { cityName } from "../utils";
import { StoreArt } from "../visuals";
import "./BusinessApplyPage.css";

export const BUSINESS_TYPES: Record<BusinessType, {label: string; emoji: string}> = {
  BAKERY: {label: "Пекарня", emoji: "🥐"},
  CAFE: {label: "Кафе", emoji: "☕"},
  RESTAURANT: {label: "Ресторан", emoji: "🍽️"},
  COFFEE_SHOP: {label: "Кофейня", emoji: "🧋"},
  CONFECTIONERY: {label: "Кондитерская", emoji: "🧁"},
  GROCERY: {label: "Магазин у дома", emoji: "🛒"},
  SUPERMARKET: {label: "Супермаркет", emoji: "🏬"},
  OTHER: {label: "Другое", emoji: "🥡"}
};

const STEPS = [
  {title: "О заведении", icon: Store},
  {title: "Адрес", icon: MapPin},
  {title: "Контакты", icon: ClipboardCheck}
];
const PHONE = /^(\+7|8)\d{10}$/;

type Props = {
  business: BusinessProfile | null;
  city: City;
  onSubmit: (data: BusinessApplication) => Promise<void>;
  onBack: () => void;
};

export function BusinessApplyPage({business, city, onSubmit, onBack}: Props) {
  const [step, setStep] = useState(0);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [locationSyncing, setLocationSyncing] = useState(false);
  const [locationStatus, setLocationStatus] = useState("");
  const [locationError, setLocationError] = useState("");
  const requestId = useRef(0);
  const skipNextAddressGeocode = useRef(false);
  const initialAddress = useRef(true);
  const [form, setForm] = useState({
    businessName: business?.businessName ?? "",
    businessType: (business?.businessType ?? "") as BusinessType | "",
    description: business?.description ?? "",
    address: business?.address ?? "",
    contactPhone: business?.contactPhone ?? "",
    bin: business?.bin ?? ""
  });
  const [location, setLocation] = useState<BusinessLocation>(() =>
    typeof business?.latitude === "number" && typeof business?.longitude === "number"
      ? {latitude: business.latitude, longitude: business.longitude} : null);
  const set = (key: keyof typeof form, value: string) => setForm(previous => ({...previous, [key]: value}));

  useEffect(() => {
    if (initialAddress.current) {
      initialAddress.current = false;
      return;
    }
    if (skipNextAddressGeocode.current) {
      skipNextAddressGeocode.current = false;
      return;
    }

    const address = form.address.trim();
    const currentRequest = ++requestId.current;
    if (!address) {
      setLocation(null);
      setLocationSyncing(false);
      setLocationStatus("");
      setLocationError("");
      return;
    }

    setLocationSyncing(true);
    setLocationStatus("Ищу адрес на карте…");
    setLocationError("");
    const timer = window.setTimeout(() => {
      void api.geocodeBusinessAddress(address).then(result => {
        if (currentRequest !== requestId.current) return;
        setLocation({latitude: result.latitude, longitude: result.longitude});
        setLocationStatus("Адрес найден, метка перемещена.");
        setLocationSyncing(false);
      }).catch((geocodingError: unknown) => {
        if (currentRequest !== requestId.current) return;
        setLocation(null);
        setLocationStatus("");
        setLocationError(geocodingError instanceof Error
          ? geocodingError.message : "Не удалось найти этот адрес.");
        setLocationSyncing(false);
      });
    }, 700);

    return () => window.clearTimeout(timer);
  }, [form.address]);

  function changeAddress(address: string) {
    requestId.current++;
    setLocation(null);
    setLocationSyncing(Boolean(address.trim()));
    setLocationStatus(address.trim() ? "Ожидаю завершения ввода…" : "");
    setLocationError("");
    set("address", address);
  }

  function changeLocation(next: BusinessLocation) {
    const currentRequest = ++requestId.current;
    setLocation(next);
    setLocationError("");
    if (!next) {
      setLocationSyncing(false);
      setLocationStatus("");
      return;
    }

    setLocationSyncing(true);
    setLocationStatus("Определяю адрес выбранной точки…");
    void api.reverseGeocodeBusinessLocation(next.latitude, next.longitude).then(result => {
      if (currentRequest !== requestId.current) return;
      if (result.address !== form.address) {
        skipNextAddressGeocode.current = true;
        set("address", result.address);
      }
      setLocationStatus("Адрес и метка синхронизированы.");
      setLocationSyncing(false);
    }).catch((geocodingError: unknown) => {
      if (currentRequest !== requestId.current) return;
      setLocationStatus("");
      setLocationError(geocodingError instanceof Error
        ? geocodingError.message : "Не удалось определить адрес этой точки.");
      setLocationSyncing(false);
    });
  }

  const approved = business?.status === "APPROVED";
  const title = !business ? "Заявка на подключение" : approved ? "Профиль заведения" : "Заявка на подключение";

  function stepError(index: number): string {
    if (index === 0) {
      if (!form.businessName.trim()) return "Укажи название заведения";
      if (!form.businessType) return "Выбери тип заведения";
    }
    if (index === 1) {
      if (locationSyncing) return "Подожди, пока адрес и карта синхронизируются";
      if (locationError) return locationError;
      if (!form.address.trim()) return "Укажи адрес";
      if (!location) return "Поставь метку заведения на карте";
    }
    if (index === 2) {
      if (!PHONE.test(form.contactPhone.trim())) return "Телефон в формате +7XXXXXXXXXX или 8XXXXXXXXXX";
    }
    return "";
  }

  function next() {
    const problem = stepError(step);
    setError(problem);
    if (!problem) setStep(s => Math.min(STEPS.length - 1, s + 1));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (step < STEPS.length - 1) { next(); return; }
    const problem = [0, 1, 2].map(stepError).find(Boolean) ?? "";
    setError(problem);
    if (problem || !location || !form.businessType || sending || locationSyncing) return;
    setSending(true);
    try {
      await onSubmit({
        businessName: form.businessName.trim(),
        businessType: form.businessType,
        description: form.description.trim(),
        address: form.address.trim(),
        latitude: location.latitude,
        longitude: location.longitude,
        contactPhone: form.contactPhone.trim(),
        bin: form.bin.trim()
      });
    } finally { setSending(false); }
  }

  return <section className="section container apply-page">
    <button type="button" className="apply-back" onClick={onBack}><ArrowLeft size={16}/> В кабинет</button>
    <div className="apply-head">
      <div>
        <span className="kicker">FoodSave for Business</span>
        <h1>{title}</h1>
        <p>{approved
          ? "Заведение уже одобрено — изменения применятся сразу."
          : "Заполни данные заведения. Заявку проверит администратор; обычно это занимает до одного рабочего дня."}</p>
        {business?.status === "REJECTED" && business.rejectionReason && <div className="apply-rejected" role="alert">
          <b>Заявка отклонена</b><span>{business.rejectionReason}</span><small>Исправь данные и отправь заявку снова.</small>
        </div>}
      </div>
      <StoreArt className="apply-art"/>
    </div>

    <ol className="apply-steps" aria-label="Шаги заявки">
      {STEPS.map(({title: stepTitle, icon: Icon}, index) => <li key={stepTitle} className={index === step ? "current" : index < step ? "done" : ""}>
        <button type="button" onClick={() => { if (index < step) setStep(index); }} disabled={index > step}>
          <span>{index < step ? <CheckCircle2 size={17}/> : <Icon size={17}/>}</span>{stepTitle}
        </button>
      </li>)}
    </ol>

    <form className="apply-card" onSubmit={event => void submit(event)} noValidate>
      <div className="apply-step" key={step}>
        {step === 0 && <>
          <label className="field"><span>Название заведения</span><input value={form.businessName} maxLength={100} onChange={e => set("businessName", e.target.value)} placeholder="Например: Пекарня «Нан»"/></label>
          <div className="field"><span>Тип заведения</span>
            <div className="type-grid" role="radiogroup" aria-label="Тип заведения">
              {(Object.keys(BUSINESS_TYPES) as BusinessType[]).map(type => <button type="button" role="radio" aria-checked={form.businessType === type} key={type}
                className={form.businessType === type ? "type-option active" : "type-option"} onClick={() => set("businessType", type)}>
                <i>{BUSINESS_TYPES[type].emoji}</i>{BUSINESS_TYPES[type].label}
              </button>)}
            </div>
          </div>
          <label className="field"><span>Описание · необязательно</span><textarea value={form.description} maxLength={1000} onChange={e => set("description", e.target.value)} placeholder="Что вы готовите, какие продукты чаще остаются в конце дня"/></label>
        </>}
        {step === 1 && <>
          <label className="field"><span>Адрес в городе {cityName(city)}</span><input value={form.address} maxLength={255} onChange={e => changeAddress(e.target.value)} placeholder="ул. Абая 10"/></label>
          <BusinessLocationPicker city={city} value={location} onChange={changeLocation}/>
          {locationStatus && <p className="location-sync-status" role="status">{locationStatus}</p>}
          {locationError && <p className="form-error" role="alert">{locationError}</p>}
        </>}
        {step === 2 && <>
          <label className="field"><span>Контактный телефон</span><input type="tel" value={form.contactPhone} onChange={e => set("contactPhone", e.target.value)} placeholder="+77001234567"/></label>
          <label className="field"><span>БИН/ИИН · необязательно</span><input value={form.bin} maxLength={12} inputMode="numeric" onChange={e => set("bin", e.target.value)} placeholder="12 цифр"/></label>
          <div className="apply-summary">
            <h3><ShieldCheck size={17}/> Проверь заявку</h3>
            <dl>
              <dt>Название</dt><dd>{form.businessName || "—"}</dd>
              <dt>Тип</dt><dd>{form.businessType ? `${BUSINESS_TYPES[form.businessType].emoji} ${BUSINESS_TYPES[form.businessType].label}` : "—"}</dd>
              <dt>Адрес</dt><dd>{form.address || "—"}{location ? ` · ${location.latitude.toFixed(4)}, ${location.longitude.toFixed(4)}` : ""}</dd>
            </dl>
            {!approved && <p>После отправки AI проверит заявку на полноту, а решение примет администратор.</p>}
          </div>
        </>}
      </div>
      {error && <div className="form-error" role="alert">{error}</div>}
      <div className="apply-actions">
        {step > 0 ? <button type="button" className="ghost-btn" onClick={() => { setError(""); setStep(s => s - 1); }}><ArrowLeft size={16}/> Назад</button> : <span/>}
        {step < STEPS.length - 1
          ? <button type="submit" className="primary-btn">Далее <ArrowRight size={16}/></button>
          : <button type="submit" className="primary-btn" disabled={sending || locationSyncing}><Send size={16}/> {sending ? "Отправляем…" : approved ? "Сохранить" : business ? "Отправить повторно" : "Отправить заявку"}</button>}
      </div>
    </form>
  </section>;
}
