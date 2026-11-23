import { useListingCover, PhotoManager } from "./pages/ListingPhotos";
import { BusinessLocationPicker, type BusinessLocation } from "./BusinessLocationPicker";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { ArrowRight, Building2, Camera, CheckCircle2, Clock3, Copy, CreditCard, Leaf, LogOut, MapPin, PackageCheck, Pencil, Plus, QrCode, RefreshCw, ScanLine, ShieldCheck, ShoppingBag, Sparkles, Star, Ticket, Trash2, UserRound, X } from "lucide-react";
import { api } from "./api";
import type { AiListingDraft, BusinessProfile, City, FoodCategory, FoodSaveQrResponse, Impact, Listing, Order, Profile } from "./types";
import { money, dt, time, disc, qrImg, errText, toLocal } from "./utils";

type Toast = {type:"success"|"error";text:string}|null;
export function Stat({v,l}:{v:string;l:string}){return <div className="stat"><b>{v}</b><span>{l}</span></div>}
export function Empty(){return <div className="empty"><ShoppingBag size={28}/><h3>Пока пусто</h3><p>Активных предложений пока нет.</p></div>}
function foodEmoji(t:string){const x=t.toLowerCase();if(x.includes("пиц"))return"🍕";if(x.includes("суш"))return"🍣";if(x.includes("бургер"))return"🍔";if(x.includes("коф"))return"☕";if(x.includes("салат"))return"🥗";if(x.includes("хлеб")||x.includes("выпеч"))return"🥐";return"🥡"}
export function ListingCard({listing:l,onOpen,recommendationReason}:{listing:Listing;onOpen:()=>void;recommendationReason?:string}){const cover=useListingCover(l.id);return <article className="listing-card"><div className="listing-image">{cover?<img className="listing-photo" src={cover} alt={l.title}/>:<span className="food-emoji">{foodEmoji(l.title)}</span>}<b className="discount">−{disc(l.originalPrice,l.foodSavePrice)}%</b><span className="available">● {l.status==="ACTIVE"?"Доступно":l.status}</span></div><div className="listing-body">{recommendationReason&&<div className="recommendation-tag"><Sparkles size={12}/>{recommendationReason}</div>}<div className="title-line"><h3>{l.title}</h3><span>×{l.quantity}</span></div><p>{l.description||"Предложение от FoodSave Business"}</p><div className="meta"><span><Building2 size={13}/> {l.businessName||"Бизнес"}</span>{l.businessRating!=null&&<span><Star size={13}/> {l.businessRating.toFixed(1)}</span>}{l.distanceKm!=null&&<span><MapPin size={13}/> {l.distanceKm.toFixed(1)} км</span>}</div><div className="pickup"><Clock3 size={14}/> {dt(l.pickupStart)} — {time(l.pickupEnd)}</div><div className="price"><div><del>{money(l.originalPrice)}</del><b>{money(l.foodSavePrice)}</b></div><button className="circle" disabled={l.status!=="ACTIVE"} onClick={onOpen} aria-label={`Открыть предложение ${l.title}`}><ArrowRight size={18}/></button></div></div></article>}
function Modal({children,close,wide=false}:{children:ReactNode;close:()=>void;wide?:boolean}){return <div className="backdrop"><div className={`modal ${wide?"wide":""}`}><button className="close" onClick={close}><X size={18}/></button>{children}</div></div>}
function Field({label,value,onChange,type="text",placeholder,required=true,min,max,minLength,maxLength,pattern}:{label:string;value:string;onChange:(v:string)=>void;type?:string;placeholder?:string;required?:boolean;min?:string;max?:string;minLength?:number;maxLength?:number;pattern?:string}){
  return <label className="field"><span>{label}</span><input type={type} value={value} placeholder={placeholder} required={required} min={min} max={max} minLength={minLength} maxLength={maxLength} pattern={pattern} step={type==="number"?(label.includes("Вес")?"0.001":label.includes("Цена")||label.includes("цена")?"0.01":"1"):undefined} onChange={e=>onChange(e.target.value)}/></label>;
}
function CitySelect({value,onChange}:{value:City;onChange:(v:City)=>void}){return <label className="field"><span>Город</span><select value={value} onChange={e=>onChange(e.target.value as City)}><option value="ALMATY">Алматы</option><option value="ASTANA">Астана</option></select></label>}

export function AuthModal({mode,loading,onClose,onMode,onLogin,onRegister}:{
  mode:"login"|"register";loading:boolean;onClose:()=>void;onMode:(mode:"login"|"register")=>void;
  onLogin:(email:string,password:string)=>void;
  onRegister:(data:{username:string;email:string;phoneNumber:string;password:string;city:City;role:"CUSTOMER"|"BUSINESS"})=>void;
}){
  const [f,setF]=useState({username:"",email:"",phoneNumber:"",password:"",city:"ALMATY" as City,role:"CUSTOMER" as "CUSTOMER"|"BUSINESS"});
  return <Modal close={onClose}>
    <div className="auth-head"><div className="modal-icon"><Leaf size={22}/></div><span className="kicker">FoodSave</span><h2>{mode==="login"?"С возвращением":"Создать аккаунт"}</h2></div>
    <form className="form-stack" onSubmit={event=>{event.preventDefault();if(mode==="login")onLogin(f.email,f.password);else onRegister(f)}}>
      {mode==="register"&&<><Field label="Username" value={f.username} onChange={v=>setF({...f,username:v})}/>
      <label className="field"><span>Тип аккаунта</span><select value={f.role} onChange={e=>setF({...f,role:e.target.value as "CUSTOMER"|"BUSINESS"})}><option value="CUSTOMER">Покупатель</option><option value="BUSINESS">Бизнес</option></select></label></>}
      <Field label="Email" type="email" value={f.email} onChange={v=>setF({...f,email:v})}/>
      {mode==="register"&&<Field label="Телефон" type="tel" value={f.phoneNumber} onChange={v=>setF({...f,phoneNumber:v})} placeholder="+77001234567" pattern="\+?[0-9]{10,15}"/>}
      <Field label="Пароль" type="password" value={f.password} onChange={v=>setF({...f,password:v})} minLength={mode==="register"?8:undefined} maxLength={64}/>
      {mode==="register"&&<CitySelect value={f.city} onChange={v=>setF({...f,city:v})}/>}
      <button className="primary-btn full" disabled={loading}>{loading?"Подождите…":mode==="login"?"Войти":"Зарегистрироваться"}<ArrowRight size={17}/></button>
    </form>
    <div className="switch">{mode==="login"?"Нет аккаунта?":"Уже зарегистрирован?"}<button onClick={()=>onMode(mode==="login"?"register":"login")}>{mode==="login"?"Создать":"Войти"}</button></div>
  </Modal>;
}
export function ProfileModal({profile:p,onClose,onSaved,onPassword,onReputation}:{profile:Profile;onClose:()=>void;onSaved:(p:Profile)=>void;onPassword:()=>void;onReputation:()=>void}){const [email,setEmail]=useState(p.email),[phone,setPhone]=useState(p.phoneNumber),[city,setCity]=useState<City>(p.city);const [error,setError]=useState("");return <Modal close={onClose}><div className="profile-top"><div className="avatar-big">{p.username[0].toUpperCase()}</div><div><span className="kicker">Мой профиль</span><h2>{p.username}</h2><span className="role">{p.role}</span></div></div><div className="profile-stats"><b>{p.foodPoints??0}<small>FoodPoints</small></b><b>{p.reliability!=null?`${p.reliability}%`:"—"}<small>Надёжность</small></b><b>{p.completedOrders??0}<small>Завершено</small></b></div><div className="profile-buttons"><button className="ghost-btn full" onClick={onPassword}><ShieldCheck size={16}/> Пароль</button>{p.role==="CUSTOMER"&&<button className="ghost-btn full" onClick={onReputation}><Star size={16}/> Репутация и FoodPoints</button>}</div><form className="form-stack" onSubmit={async e=>{e.preventDefault();setError("");try{onSaved(await api.updateProfile({email,phoneNumber:phone,city}))}catch(x){setError(errText(x,"Не удалось сохранить"))}}}><Field label="Email" type="email" value={email} onChange={setEmail}/><Field label="Телефон" value={phone} onChange={setPhone}/><CitySelect value={city} onChange={setCity}/><div className="locked"><UserRound size={16}/> Username: <b>{p.username}</b><span>не изменяется</span></div>{error&&<div className="form-error">{error}</div>}<button className="primary-btn full">Сохранить изменения</button></form></Modal>}
export function PasswordModal({close,toast}:{close:()=>void;toast:(t:Toast)=>void}){const [a,setA]=useState(""),[b,setB]=useState("");return <Modal close={close}><span className="kicker">Безопасность</span><h2>Сменить пароль</h2><p className="muted">Минимум 8 символов — такая же проверка, как на backend.</p><form className="form-stack" onSubmit={async e=>{e.preventDefault();if(b.length<8||b.length>64){toast({type:"error",text:"Новый пароль должен содержать от 8 до 64 символов"});return}try{await api.changePassword({oldPassword:a,newPassword:b});toast({type:"success",text:"Пароль изменён"});close()}catch(x){toast({type:"error",text:errText(x,"Ошибка")})}}}><Field label="Старый пароль" type="password" value={a} onChange={setA}/><Field label="Новый пароль" type="password" value={b} onChange={setB} min="8" max="64"/><button className="primary-btn full">Изменить</button></form></Modal>}

const FOOD_CATEGORY_OPTIONS: Record<"ru"|"kk"|"en",Array<{value:FoodCategory;label:string}>> = {
  ru:[
    {value:"BAKERY",label:"Выпечка"},{value:"READY_MEALS",label:"Готовые блюда"},{value:"DESSERTS",label:"Десерты"},
    {value:"DAIRY",label:"Молочные продукты"},{value:"FRUITS_VEGETABLES",label:"Фрукты и овощи"},{value:"MEAT_FISH",label:"Мясо и рыба"},
    {value:"GROCERIES",label:"Бакалея"},{value:"DRINKS",label:"Напитки"},{value:"OTHER",label:"Другое"}
  ],
  kk:[
    {value:"BAKERY",label:"Нан-тоқаш өнімдері"},{value:"READY_MEALS",label:"Дайын тағамдар"},{value:"DESSERTS",label:"Тәттілер"},
    {value:"DAIRY",label:"Сүт өнімдері"},{value:"FRUITS_VEGETABLES",label:"Жемістер мен көкөністер"},{value:"MEAT_FISH",label:"Ет пен балық"},
    {value:"GROCERIES",label:"Азық-түлік"},{value:"DRINKS",label:"Сусындар"},{value:"OTHER",label:"Басқа"}
  ],
  en:[
    {value:"BAKERY",label:"Bakery"},{value:"READY_MEALS",label:"Ready meals"},{value:"DESSERTS",label:"Desserts"},
    {value:"DAIRY",label:"Dairy"},{value:"FRUITS_VEGETABLES",label:"Fruit & vegetables"},{value:"MEAT_FISH",label:"Meat & fish"},
    {value:"GROCERIES",label:"Groceries"},{value:"DRINKS",label:"Drinks"},{value:"OTHER",label:"Other"}
  ]
};

export function ListingModal({existing,close,submit}:{existing:Listing|null;close:()=>void;submit:(d:{title:string;description:string;originalPrice:number;foodSavePrice:number;quantity:number;weightKg:number;pickupStart:string;pickupEnd:string;expiresAt:string;productExpiresAt:string;category:FoodCategory},photos:File[])=>void}){
  const defaultStart = new Date(Math.ceil((Date.now()+5*60000)/(15*60000))*(15*60000));
  const start=existing?new Date(existing.pickupStart):defaultStart;
  const end=existing?new Date(existing.pickupEnd):new Date(defaultStart.getTime()+3*3600000);
  const defaultListingEnd=new Date(end.getTime()+15*60000);
  const [f,setF]=useState({
    title:existing?.title||"",description:existing?.description||"",
    originalPrice:String(existing?.originalPrice??""),foodSavePrice:String(existing?.foodSavePrice??""),
    quantity:String(existing?.quantity??""),weightKg:String(existing?.weightKg??""),
    pickupStart:toLocal(start),pickupEnd:toLocal(end),expiresAt:toLocal(existing?new Date(existing.expiresAt):defaultListingEnd)
  });
  const [category,setCategory]=useState<FoodCategory>(existing?.category??"OTHER");
  const [photos,setPhotos]=useState<File[]>([]);
  const [photoError,setPhotoError]=useState("");
  const [productExpiresAt,setProductExpiresAt]=useState(existing?.productExpiresAt?toLocal(new Date(existing.productExpiresAt)):"");
  const [aiNotes,setAiNotes]=useState("");
  const [aiLanguage,setAiLanguage]=useState<"ru"|"kk"|"en">("ru");
  const [aiLoading,setAiLoading]=useState(false);
  const [aiError,setAiError]=useState("");
  const [aiDraft,setAiDraft]=useState<AiListingDraft|null>(null);
  const photoPreviews=useMemo(()=>photos.map(file=>URL.createObjectURL(file)),[photos]);
  useEffect(()=>()=>photoPreviews.forEach(url=>URL.revokeObjectURL(url)),[photoPreviews]);
  const set=(k:string,v:string)=>setF(previous=>({...previous,[k]:v}));

  const addPhotos=(event:React.ChangeEvent<HTMLInputElement>)=>{
    const incoming=Array.from(event.target.files||[]);
    const invalid=incoming.some(file=>file.size>5*1024*1024||!['image/jpeg','image/png'].includes(file.type));
    if(incoming.length+photos.length>5||invalid){setPhotoError("Разрешены максимум 5 корректных JPG/PNG размером до 5 МБ каждый");event.target.value="";return;}
    setPhotoError("");
    if(incoming.length){setPhotos(previous=>[...previous,...incoming]);setAiDraft(null);setAiError("");}
    event.target.value="";
  };
  const removePhoto=(index:number)=>{setPhotos(previous=>previous.filter((_,i)=>i!==index));setAiDraft(null);setAiError("");};

  const analyzePhoto=async()=>{
    if(!photos.length){setAiError("Сначала загрузи хотя бы одну фотографию продукта");return;}
    setAiLoading(true);setAiError("");
    try{
      const result=await api.analyzeAiListing(photos[0],aiLanguage,aiNotes);
      setAiDraft(result);
      setF(previous=>({...previous,title:result.title,description:result.description}));
      setCategory(result.suggestedCategory||"OTHER");
    }catch(error){setAiError(errText(error,"Не удалось проанализировать фотографию"));}
    finally{setAiLoading(false);}
  };

  const submitForm=(e:React.FormEvent<HTMLFormElement>)=>{
    e.preventDefault();
    const now=Date.now(),s=new Date(f.pickupStart).getTime(),en=new Date(f.pickupEnd).getTime(),ex=new Date(f.expiresAt).getTime();
    if(!f.title.trim()){alert("Укажи название продукта");return;}
    if(!Number.isFinite(+f.originalPrice)||!Number.isFinite(+f.foodSavePrice)||!Number.isFinite(+f.quantity)||!Number.isFinite(+f.weightKg)
      ||+f.originalPrice<=+f.foodSavePrice||+f.foodSavePrice<=0||+f.quantity<(existing?0:1)||+f.weightKg<=0){alert("Проверь цены, количество и вес");return;}
    if(s<now||en<now||ex<now){alert("Дата и время предложения не могут быть в прошлом");return;}
    if(s>en){alert("Забрать с не может быть позже Забрать до");return;}
    if(ex<en){alert("Истекает не может быть раньше окончания получения");return;}
    if(!productExpiresAt){alert("Укажи срок годности продукта — это обязательное поле");return;}
    const expiry=new Date(productExpiresAt).getTime();
    if(!Number.isFinite(expiry)||expiry<=en+15*60000){alert("Продукт должен быть годен минимум 15 минут после выдачи");return;}
    if(ex>expiry){alert("Срок действия предложения не может превышать срок годности продукта");return;}
    submit({...f,category,productExpiresAt,originalPrice:+f.originalPrice,foodSavePrice:+f.foodSavePrice,quantity:+f.quantity,weightKg:+f.weightKg},photos);
  };

  const labelRows=aiDraft?.label?[
    ["Бренд",aiDraft.label.brand],["Вес на упаковке",aiDraft.label.netWeight],["Состав",aiDraft.label.ingredients],
    ["Аллергены",aiDraft.label.allergens],["Дата на упаковке",aiDraft.label.expiryDateText],["Хранение",aiDraft.label.storageConditions]
  ].filter((row):row is [string,string]=>Boolean(row[1])):[];

  return <Modal close={close} wide>
    <span className="kicker">FoodSave Business</span><h2>{existing?"Редактировать предложение":"Новое предложение"}</h2>
    <form className="form-grid" onSubmit={submitForm}>
      <section className="span ai-listing-panel ai-listing-panel-v2">
        <div className="ai-listing-heading"><span className="ai-sparkle"><Sparkles size={18}/></span><div><strong>AI Listing Assistant</strong><small>Фото → готовый черновик</small></div><span className="ai-version">FS-010</span></div>
        <p>Загрузи фото продукта. FoodSave распознает товар, прочитает этикетку и подготовит название, описание и категорию.</p>
        <label className="field ai-upload-label"><span>Фотографии продукта · до 5 JPG/PNG по 5 МБ</span><input type="file" accept="image/jpeg,image/png" multiple onChange={addPhotos}/></label>
        {!!photos.length&&<div className="ai-photo-grid">{photos.map((photo,index)=><div className="ai-photo-card" key={`${photo.name}-${index}`}>
          <img src={photoPreviews[index]} alt={`Фото продукта ${index+1}`}/><span>{index===0?"Для AI-анализа":"Фото объявления"}</span>
          <button type="button" aria-label="Удалить фотографию" onClick={()=>removePhoto(index)}><X size={14}/></button>
        </div>)}</div>}
        {photoError&&<p className="ai-listing-error" role="alert">{photoError}</p>}
        <div className="ai-settings-row">
          <label className="field"><span>Язык черновика</span><select value={aiLanguage} onChange={e=>{setAiLanguage(e.target.value as "ru"|"kk"|"en");setAiDraft(null);}}><option value="ru">Русский</option><option value="kk">Қазақша</option><option value="en">English</option></select></label>
          <div className="ai-ocr-chip"><ScanLine size={15}/> OCR включён</div><div className="ai-ocr-chip"><span>RU</span><span>KK</span><span>EN</span> Multilingual</div>
        </div>
        <label className="field"><span>Подсказка для AI · необязательно</span><textarea maxLength={600} value={aiNotes} onChange={e=>setAiNotes(e.target.value)} placeholder="Например: это набор из трёх круассанов с шоколадом."/></label>
        <button type="button" className="primary-btn ai-analyze-btn" disabled={aiLoading||!photos.length} onClick={()=>void analyzePhoto()}>
          {aiLoading?<><RefreshCw className="spin" size={16}/> Анализируем фото…</>:<><Sparkles size={16}/> Создать черновик по фото</>}
        </button>
        {aiError&&<p className="ai-listing-error" role="alert">{aiError}</p>}
        {aiDraft&&<div className="ai-draft-result">
          <div className="ai-result-title"><CheckCircle2 size={18}/><div><strong>Черновик подготовлен</strong><small>Проверь данные и отредактируй перед публикацией</small></div></div>
          <div className="ai-detected-product"><span>AI распознал</span><b>{aiDraft.detectedProduct}</b></div>
          <div className="ai-ocr-result"><div><ScanLine size={15}/><strong>Распознанный текст упаковки</strong></div><p>{aiDraft.ocrText||"Текст упаковки не удалось уверенно прочитать."}</p></div>
          {!!labelRows.length&&<div className="ai-label-grid">{labelRows.map(([label,value])=><div key={label}><span>{label}</span><b>{value}</b></div>)}</div>}
          {aiDraft.label.expiryDateText&&<p className="ai-safety-note"><ShieldCheck size={15}/> Дата прочитана с упаковки. Введи её вручную в поле «Годен до» и проверь перед публикацией.</p>}
          {!!aiDraft.warnings.length&&<div className="ai-warnings"><strong><ShieldCheck size={15}/> Требует проверки</strong><ul>{aiDraft.warnings.map((warning,index)=><li key={index}>{warning}</li>)}</ul></div>}
        </div>}
      </section>
      <div className="span"><Field label="Название" value={f.title} onChange={v=>set("title",v)} maxLength={120} placeholder="Название продукта"/></div>
      <div className="span"><label className="field"><span>Описание — можно редактировать</span><textarea maxLength={1000} value={f.description} onChange={e=>set("description",e.target.value)} placeholder="Описание продукта"/></label></div>
      <label className="field"><span>Категория</span><select value={category} onChange={e=>setCategory(e.target.value as FoodCategory)}>{FOOD_CATEGORY_OPTIONS[aiLanguage].map(option=><option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
      <Field label="Обычная цена" type="number" min="1" value={f.originalPrice} onChange={v=>set("originalPrice",v)}/>
      <Field label="Цена FoodSave" type="number" min="1" value={f.foodSavePrice} onChange={v=>set("foodSavePrice",v)}/>
      <Field label="Количество" type="number" min={existing?"0":"1"} value={f.quantity} onChange={v=>set("quantity",v)}/>
      <div><Field label="Вес 1 шт. (кг)" type="number" min="0.001" value={f.weightKg} onChange={v=>set("weightKg",v)}/>{aiDraft?.label.netWeight&&<p className="ai-field-hint">На упаковке распознано: {aiDraft.label.netWeight}. Проверь и введи вес вручную.</p>}</div>
      <Field label="Забрать с" type="datetime-local" value={f.pickupStart} onChange={v=>set("pickupStart",v)}/>
      <Field label="Забрать до" type="datetime-local" value={f.pickupEnd} onChange={v=>set("pickupEnd",v)}/>
      <div className="span"><Field label="Истекает (срок действия объявления)" type="datetime-local" value={f.expiresAt} onChange={v=>set("expiresAt",v)}/></div>
      <div className="span"><Field label="Годен до (срок продукта; подтверждает продавец)" type="datetime-local" required value={productExpiresAt} onChange={setProductExpiresAt}/><p className="muted">Обязательно: продукт должен оставаться годным во время выдачи и ещё 15 минут. AI не заполняет это поле автоматически.</p></div>
      <div className="span">{existing&&<><h3>Загруженные фотографии</h3><PhotoManager listingId={existing.id}/></>}</div>
      <div className="form-end span"><button type="button" className="ghost-btn" onClick={close}>Отмена</button><button className="primary-btn" disabled={aiLoading}>{existing?"Сохранить":"Опубликовать"}<ArrowRight size={16}/></button></div>
    </form>
  </Modal>;
}

export function OrderModal({listing:l,close,submit}:{listing:Listing;close:()=>void;submit:(l:Listing,q:number)=>void}){const [q,setQ]=useState(1);return <Modal close={close}><div className="order-preview"><span>{foodEmoji(l.title)}</span><b>−{disc(l.originalPrice,l.foodSavePrice)}%</b></div><span className="kicker">{l.businessName||"FoodSave Business"}</span><h2>{l.title}</h2><p className="muted">{l.description}</p><div className="details"><span><Clock3 size={14}/> {dt(l.pickupStart)} — {time(l.pickupEnd)}</span><span><MapPin size={14}/> {l.distanceKm!=null?`${l.distanceKm.toFixed(1)} км`:"рядом"}</span><span><Star size={14}/> {l.businessRating?.toFixed(1)||"—"}</span></div><div className="qty"><span>Количество</span><div><button onClick={()=>setQ(Math.max(1,q-1))}>−</button><b>{q}</b><button onClick={()=>setQ(Math.min(l.quantity,q+1))}>+</button></div></div><div className="checkout"><div><small>Итого</small><b>{money(l.foodSavePrice*q)}</b></div><button className="primary-btn" onClick={()=>submit(l,q)}>Забронировать <ArrowRight size={16}/></button></div><div className="note">Сначала бронирование, затем оплата в разделе «Мои заказы».</div></Modal>}
export function QrModal({qr,close}:{qr:string;close:()=>void}){return <Modal close={close}><div className="qr-modal"><span className="kicker">FoodSave QR</span><h2>Покажи QR при получении</h2><p>После сканирования бизнесом заказ станет COMPLETED.</p><div className="qr-box"><img src={qrImg(qr)} alt="Order QR"/></div><code>{qr}</code><button className="ghost-btn full" onClick={()=>navigator.clipboard?.writeText(qr)}><Copy size={15}/> Скопировать</button></div></Modal>}
export function ReviewModal({order:o,close,submit}:{order:Order;close:()=>void;submit:(o:Order,r:number,c:string)=>void}){const [r,setR]=useState(5),[c,setC]=useState("");return <Modal close={close}><span className="kicker">Отзыв</span><h2>{o.listingTitle}</h2><div className="stars">{[1,2,3,4,5].map(n=><button key={n} className={n<=r?"on":""} onClick={()=>setR(n)}><Star size={30} fill="currentColor"/></button>)}</div><label className="field"><span>Комментарий</span><textarea value={c} onChange={e=>setC(e.target.value)} placeholder="Как прошёл заказ?"/></label><button className="primary-btn full" onClick={()=>submit(o,r,c)}>Оставить отзыв</button></Modal>}
export function ImpactModal({data,loading,error,completedOrders,foodPoints,onRetry,close}:{
  data:Impact|null;
  loading:boolean;
  error:string|null;
  completedOrders:number;
  foodPoints:number;
  onRetry:()=>void;
  close:()=>void;
}){
  return <Modal close={close}>
    <span className="kicker">FoodSave Impact</span>
    <h2>Твой вклад</h2>
    <p className="muted impact-intro">Здесь отображаются результаты заказов, которые уже были получены и завершены.</p>
    <div className="impact-profile-stats" aria-label="Статистика аккаунта">
      <div><b>{completedOrders}</b><span>Завершено заказов</span></div>
      <div><b>{foodPoints}</b><span>FoodPoints</span></div>
    </div>
    {loading && <div className="impact-state" role="status">Загружаем расширенную статистику…</div>}
    {!loading && data && <>
      <h3 className="impact-section-title">Экологический эффект</h3>
      <div className="impact-big">
        <div><b>{data.foodSavedKg.toLocaleString("ru-RU",{maximumFractionDigits:2})}</b><span>кг еды спасено</span></div>
        <div><b>{money(data.moneySaved)}</b><span>сэкономлено</span></div>
        <div><b>{data.co2SavedKg.toLocaleString("ru-RU",{maximumFractionDigits:2})}</b><span>кг CO₂e (оценочно)</span></div>
      </div>
      <p className="muted impact-footnote">Расчёт экологического эффекта предоставляется backend на основе завершённых заказов.</p>
    </>}
    {!loading && error && <div className="impact-error" role="alert">
      <b>Не удалось получить расширенную статистику</b>
      <p>{error.length>240?`${error.slice(0,240)}…`:error}</p>
      <p>Статистика аккаунта выше доступна из профиля. Данные о сэкономленных деньгах, весе еды и CO₂ нельзя достоверно посчитать без API.</p>
      <button className="ghost-btn" onClick={onRetry} type="button"><RefreshCw size={16}/> Повторить запрос</button>
    </div>}
  </Modal>;
}
export function BusinessModal({
  business: b,
  city,
  close,
  save,
}: {
  business: BusinessProfile | null;
  city: City;
  close: () => void;
  save: (data: {
    businessName: string;
    description: string;
    address: string;
    latitude: number;
    longitude: number;
  }) => void;
}) {
  const [name, setName] = useState(b?.businessName || "");
  const [description, setDescription] = useState(b?.description || "");
  const [address, setAddress] = useState(b?.address || "");
  const [location, setLocation] = useState<BusinessLocation>(() => {
    const latitude = b?.latitude;
    const longitude = b?.longitude;
    if (
      typeof latitude === "number" &&
      typeof longitude === "number" &&
      Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude >= -90 && latitude <= 90 &&
      longitude >= -180 && longitude <= 180
    ) {
      return { latitude, longitude };
    }
    return null;
  });

  return (
    <Modal close={close} wide>
      <span className="kicker">Business Profile</span>
      <h2>{b ? "Изменить профиль" : "Создать профиль"}</h2>
      <p className="muted">Укажи адрес и отметь точное место заведения на карте — покупатели смогут найти его.</p>
      <form
        className="form-stack"
        onSubmit={event => {
          event.preventDefault();
          if (!location) return;
          save({
            businessName: name,
            description,
            address,
            latitude: location.latitude,
            longitude: location.longitude,
          });
        }}
      >
        <Field label="Название бизнеса" value={name} onChange={setName} />
        <Field label="Описание" value={description} onChange={setDescription} required={false} />
        <Field label="Адрес" value={address} onChange={setAddress} placeholder="ул. Абая 10" />
        <BusinessLocationPicker city={city} value={location} onChange={setLocation} />
        {!location && <p className="business-location-hint">Поставь метку на карте, чтобы сохранить координаты бизнеса.</p>}
        <button className="primary-btn full" disabled={!location}>Сохранить профиль</button>
      </form>
    </Modal>
  );
}
export function ScannerModal({close,scan}:{close:()=>void;scan:(token:string)=>void}) {
  const [value,setValue]=useState("");
  const [running,setRunning]=useState(false);
  const videoRef=useRef<HTMLVideoElement>(null);
  const streamRef=useRef<MediaStream|null>(null);
  const frameRef=useRef<number|null>(null);
  const activeRef=useRef(false);
  function stop() {
    activeRef.current=false;
    if(frameRef.current!==null)cancelAnimationFrame(frameRef.current);
    streamRef.current?.getTracks().forEach(track=>track.stop());
    streamRef.current=null;
    if(videoRef.current) videoRef.current.srcObject=null;
    setRunning(false);
  }
  useEffect(()=>()=>{
    activeRef.current=false;
    if(frameRef.current!==null)cancelAnimationFrame(frameRef.current);
    streamRef.current?.getTracks().forEach(track=>track.stop());
  },[]);
  async function camera(){
    const Detector=(window as any).BarcodeDetector;
    if(!Detector){alert("BarcodeDetector не поддерживается в этом браузере. Используй ручной ввод.");return;}
    try {
      stop();
      const stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:"environment"}});
      streamRef.current=stream;
      if(!videoRef.current){stream.getTracks().forEach(track=>track.stop());return;}
      videoRef.current.srcObject=stream;
      await videoRef.current.play();
      activeRef.current=true;
      setRunning(true);
      const detector=new Detector({formats:["qr_code"]});
      const scanFrame=async()=>{
        if(!activeRef.current||!videoRef.current)return;
        try {
          const codes=await detector.detect(videoRef.current);
          if(codes[0]?.rawValue){
            const raw=String(codes[0].rawValue);
            stop();
            scan(raw.includes("foodsave://order/")?raw.split("foodsave://order/")[1]:raw);
            return;
          }
        }catch{ /* камера может быть между кадрами */ }
        if(activeRef.current)frameRef.current=requestAnimationFrame(scanFrame);
      };
      frameRef.current=requestAnimationFrame(scanFrame);
    }catch{stop();alert("Не удалось открыть камеру. Проверь разрешение браузера.");}
  }
  return <Modal close={()=>{stop();close()}}>
    <span className="kicker">Business</span><h2>Сканировать заказ</h2>
    <p className="muted">Можно использовать камеру или вставить qrToken вручную.</p>
    <video ref={videoRef} className={running?"scanner-video":"scanner-video hidden"} playsInline muted/>
    <button className="primary-btn full" onClick={camera}><Camera size={17}/> {running?"Перезапустить камеру":"Открыть камеру"}</button>
    <div className="or">или</div>
    <Field label="QR data / token" value={value} onChange={setValue}/>
    <button className="ghost-btn full" disabled={!value.trim()} onClick={()=>{stop();scan(value.includes("foodsave://order/")?value.split("foodsave://order/")[1]:value)}}><CheckCircle2 size={16}/> Подтвердить заказ</button>
  </Modal>;
}
export function Toast({t}:{t:Exclude<Toast,null>}){return <div className={`toast ${t.type}`}><span/> {t.text}</div>}
export function Landing({onAuth}:{onAuth:(m:"login"|"register")=>void}){return <div className="landing"><header className="topbar"><div className="container nav"><div className="brand"><span className="brand-mark"><Leaf size={18}/></span>Food<span>Save</span></div><div className="actions"><button className="ghost-btn" onClick={()=>onAuth("login")}>Войти</button><button className="primary-btn" onClick={()=>onAuth("register")}>Создать аккаунт</button></div></div></header><section className="landing-hero container"><div><span className="eyebrow"><Leaf size={15}/> Алматы · Астана · Казахстан</span><h1>Еда, которую<br/><em>успеют спасти.</em></h1><p>FoodSave соединяет людей и бизнес, чтобы хорошая еда не превращалась в отходы.</p><div className="hero-actions"><button className="primary-btn" onClick={()=>onAuth("register")}>Начать спасать <ArrowRight size={17}/></button><button className="ghost-btn" onClick={()=>onAuth("login")}>У меня уже есть аккаунт</button></div></div><div className="landing-art"><div className="landing-orb"/><div className="landing-food"><span>🥐</span></div><div className="landing-card c1">−75%<small>скидка</small></div><div className="landing-card c2">рядом<small>по геолокации</small></div><div className="landing-card c3">QR<small>получение заказа</small></div></div></section><section className="landing-bottom"><div className="container values"><Value icon={<Ticket/>} title="До 90% дешевле" text="Спасай еду со скидкой."/><Value icon={<MapPin/>} title="Реальная карта" text="Предложения показываются по координатам бизнеса."/><Value icon={<Leaf/>} title="Impact" text="Завершённые заказы формируют статистику."/></div></section></div>}
function Value({icon,title,text}:{icon:ReactNode;title:string;text:string}){return <div className="value"><span>{icon}</span><div><b>{title}</b><p>{text}</p></div></div>}
