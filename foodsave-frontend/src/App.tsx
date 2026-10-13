import { useEffect, useMemo, useState } from "react";
import { Leaf, LogOut, RefreshCw, ScanLine } from "lucide-react";
import { api } from "./api";
import { parseImpact } from "./impact";
import type { BusinessProfile, City, FoodSaveQrResponse, Impact, Listing, Order, Profile, Review } from "./types";
import { errText, confirmedPayment, coordinatesCity, listingsForCity, listingAvailable, listingCity, orderPaidByServer, cityName } from "./utils";
import { AuthModal, BusinessModal, ImpactModal, Landing, ListingModal, OrderModal, PasswordModal, ProfileModal, QrModal, ReviewModal, ScannerModal, Toast } from "./components";
import { DiscoverPage } from "./pages/DiscoverPage";
import { OrdersPage } from "./pages/OrdersPage";
import { BusinessPage } from "./pages/BusinessPage";

type Route = "/discover" | "/orders" | "/business";
type Dialog = "profile" | "password" | "listing" | "business" | "order" | "qr" | "review" | "scanner" | "impact" | null;
type ToastValue = {type: "success" | "error"; text: string} | null;
const roleHome = (profile: Profile): Route => profile.role === "BUSINESS" ? "/business" : "/discover";
function readRoute(): Route | null {
  if (location.pathname === "/discover") return "/discover";
  if (location.pathname === "/orders") return "/orders";
  if (location.pathname === "/business") return "/business";
  return null;
}
function readPaidIds(userId: string): Set<string> {
  try {
    const stored: unknown = JSON.parse(localStorage.getItem(`foodsave_paid_${userId}`) || "[]");
    return new Set(Array.isArray(stored) ? stored.filter(x => typeof x === "string") : []);
  } catch { return new Set(); }
}

export default function App() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [authenticated, setAuthenticated] = useState(() => Boolean(localStorage.getItem("foodsave_token")));
  const [initializing, setInitializing] = useState(() => Boolean(localStorage.getItem("foodsave_token")));
  const [route, setRoute] = useState<Route | null>(readRoute);
  const [listings, setListings] = useState<Listing[]>([]);
  const [myListings, setMyListings] = useState<Listing[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [business, setBusiness] = useState<BusinessProfile | null>(null);
  const [rating, setRating] = useState<number | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [impact, setImpact] = useState<Impact | null>(null);
  const [impactLoading, setImpactLoading] = useState(false);
  const [impactError, setImpactError] = useState<string | null>(null);
  const [dialog, setDialog] = useState<Dialog>(null);
  const [selected, setSelected] = useState<Listing | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [qr, setQr] = useState<FoodSaveQrResponse | null>(null);
  const [qrKind, setQrKind] = useState<"order"|"profile">("order");
  const [toast, setToast] = useState<ToastValue>(null);
  const [busy, setBusy] = useState(false);
  const [locating, setLocating] = useState(false);
  const [payingId, setPayingId] = useState<string | null>(null);
  const [paidIds, setPaidIds] = useState<Set<string>>(new Set());
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authOpen, setAuthOpen] = useState(false);
  const [nearby, setNearby] = useState(false);
  const [coords, setCoords] = useState<{lat:number;lon:number} | null>(null);

  function navigate(next: Route, replace = false) {
    if (location.pathname !== next) {
      if (replace) history.replaceState({}, "", next);
      else history.pushState({}, "", next);
    }
    setRoute(next);
    window.scrollTo({top: 0, behavior: "auto"});
  }
  useEffect(() => {
    const handlePop = () => setRoute(readRoute());
    window.addEventListener("popstate", handlePop);
    return () => window.removeEventListener("popstate", handlePop);
  }, []);
  useEffect(() => {
    if (authenticated) void refresh(true);
    else setInitializing(false);
    // Выполняется один раз при старте. После логина refresh вызывается явно.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timeout = setTimeout(() => setToast(null), 3700);
    return () => clearTimeout(timeout);
  }, [toast]);
  useEffect(() => {
    if (!profile) return;
    const target = roleHome(profile);
    const validRoute = profile.role === "BUSINESS" ? route === "/business" : route === "/discover" || route === "/orders";
    if (!validRoute) navigate(target, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile?.role, route]);

  async function refresh(firstLoad = false) {
    setBusy(true);
    try {
      const person = await api.getProfile();
      setProfile(person);
      setPaidIds(readPaidIds(person.id));
      if (person.role === "BUSINESS") {
        // Никогда не используем публичный /listings для кабинета бизнеса.
        const [biz, mine, userRating, userReviews] = await Promise.all([
          api.getBusinessProfile().catch(error => {
            if (/404|business profile not found|профиль бизнеса не найден/i.test(String(error))) return null;
            throw error;
          }),
          api.getMyListings(),
          api.getMyRating().catch(() => null),
          api.getMyReviews().catch(() => [])
        ]);
        setBusiness(biz);
        setMyListings(mine.filter(item => !item.ownerId || item.ownerId === person.id || item.ownerId === biz?.id));
        setRating(userRating);
        setReviews(userReviews);
        setListings([]);
        setOrders([]);
      } else {
        const validNearby = nearby && coords && coordinatesCity(coords.lat, coords.lon) === person.city;
        if (nearby && !validNearby) setNearByOff();
        const [all, myOrders] = await Promise.all([
          validNearby && coords
            ? api.getNearbyListings(coords.lat, coords.lon, 10, person.city) : api.getListings(person.city),
          api.getMyOrders()
        ]);
        setListings(all);
        setOrders(myOrders);
        setBusiness(null);
        setMyListings([]);
        setReviews([]);
        setRating(null);
      }
    } catch (error) {
      const message = errText(error, "Ошибка загрузки данных");
      if (/HTTP 401|HTTP 403|unauthorized|invalid token/i.test(message) && firstLoad) {
        localStorage.removeItem("foodsave_token");
        setAuthenticated(false);
        setProfile(null);
      } else setToast({type:"error",text:message});
    } finally {
      setBusy(false);
      setInitializing(false);
    }
  }

  async function login(email:string,password:string) {
    setBusy(true);
    try {
      const result = await api.login({email,password});
      localStorage.setItem("foodsave_token",result.token);
      setAuthenticated(true);
      setAuthOpen(false);
      await refresh();
      setToast({type:"success",text:"Добро пожаловать в FoodSave"});
    } catch(error) {setToast({type:"error",text:errText(error,"Не удалось войти")});}
    finally {setBusy(false);}
  }
  async function register(data:{username:string;email:string;phoneNumber:string;password:string;city:City;role:"CUSTOMER"|"BUSINESS"}) {
    setBusy(true);
    try {
      const result = await api.register(data);
      localStorage.setItem("foodsave_token",result.token);
      setAuthenticated(true);
      setAuthOpen(false);
      await refresh();
      setToast({type:"success",text:"Аккаунт создан"});
    } catch(error) {setToast({type:"error",text:errText(error,"Не удалось зарегистрироваться")});}
    finally {setBusy(false);}
  }
  function logout() {
    localStorage.removeItem("foodsave_token");
    setAuthenticated(false);setProfile(null);setListings([]);setOrders([]);setMyListings([]);setBusiness(null);
    setPaidIds(new Set());setQr(null);setDialog(null);setImpact(null);setImpactError(null);setNearByOff();
    history.replaceState({}, "", "/");setRoute(null);
  }
  function setNearByOff() {setNearby(false);setCoords(null);}
  async function showAllListings() {
    if (!profile) return;
    setNearByOff();
    setBusy(true);
    try {setListings(await api.getListings(profile.city));}
    catch(error){setToast({type:"error",text:errText(error,"Не удалось загрузить предложения")});}
    finally{setBusy(false);}
  }
  function locate() {
    if (!navigator.geolocation) {setToast({type:"error",text:"Браузер не поддерживает геолокацию"});return;}
    setLocating(true);
    navigator.geolocation.getCurrentPosition(async position => {
      const current={lat:position.coords.latitude,lon:position.coords.longitude};
      try {
        const geoCity = coordinatesCity(current.lat, current.lon);
        if (geoCity !== profile?.city) {
          setToast({type:"error",text:geoCity
            ? `Сейчас ты находишься в городе ${cityName(geoCity)}, а в профиле выбран ${cityName(profile!.city)}. Измени город в профиле для поиска рядом.`
            : "Не удалось определить город по координатам. Проверь геолокацию или открой предложения своего города."});
          return;
        }
        const nearbyListings=await api.getNearbyListings(current.lat,current.lon,10,profile.city);
        setCoords(current);setNearby(true);setListings(nearbyListings);
      } catch(error){setToast({type:"error",text:errText(error,"Ошибка загрузки ближайших предложений")});}
      finally {setLocating(false);}
    },()=>{setLocating(false);setToast({type:"error",text:"Разреши доступ к геолокации"});},{enableHighAccuracy:true,timeout:10000});
  }

  const customerListings=useMemo(() => {
    if (!profile) return [];
    return listingsForCity(listings, profile.city);
  }, [listings,profile]);
  const hiddenUnknown=useMemo(() => listings.filter(item => listingAvailable(item) && listingCity(item) === null).length,[listings]);
  const confirmedPaid = useMemo(() => new Set(orders.filter(order => orderPaidByServer(order) || (!order.paid && !order.paymentStatus && paidIds.has(order.id))).map(order=>order.id)),[orders,paidIds]);

  async function createOrder(listing:Listing,quantity:number) {
    try {
      await api.createOrder(listing.id,quantity);
      setSelected(null);setDialog(null);
      await refresh();
      navigate("/orders");
      setToast({type:"success",text:"Заказ создан. Теперь его можно оплатить."});
    }catch(error){setToast({type:"error",text:errText(error,"Не удалось забронировать")});}
  }
  async function pay(order:Order) {
    if (payingId) return;
    setPayingId(order.id);
    try {
      const payment=await api.payForOrder(order.id);
      if (!confirmedPayment(payment?.status)) throw new Error(`Платёж не подтверждён. Статус: ${payment?.status || "неизвестен"}`);
      const userId=profile!.id;
      const updated=new Set(readPaidIds(userId));updated.add(order.id);
      localStorage.setItem(`foodsave_paid_${userId}`,JSON.stringify([...updated]));
      setPaidIds(updated);
      setToast({type:"success",text:"Оплата подтверждена. QR доступен в заказе."});
      // Ошибка получения QR не откатывает подтверждённую оплату.
      try {const orderQr=await api.getOrderQr(order.id);setQr({fsId:order.fsId,qrData:orderQr.qrData});setSelectedOrder(order);setQrKind("order");setDialog("qr");}
      catch {setToast({type:"success",text:"Оплачено. QR можно открыть позже из заказа."});}
      await refresh();
    }catch(error){setToast({type:"error",text:errText(error,"Не удалось оплатить")});}
    finally {setPayingId(null);}
  }
  async function showOrderQr(order:Order){
    try {const orderQr=await api.getOrderQr(order.id);setQr({fsId:order.fsId,qrData:orderQr.qrData});setSelectedOrder(order);setQrKind("order");setDialog("qr");}
    catch(error){setToast({type:"error",text:errText(error,"QR пока недоступен")});}
  }
  async function cancelOrder(order:Order){
    if (!window.confirm("Отменить бронирование?")) return;
    try {await api.cancelOrder(order.id);await refresh();setToast({type:"success",text:"Заказ отменён"});}
    catch(error){setToast({type:"error",text:errText(error,"Не удалось отменить")});}
  }
  async function loadImpact(){
    setDialog("impact");
    setImpactLoading(true);
    setImpactError(null);
    setImpact(null);
    try {
      const response = await api.getImpact();
      setImpact(parseImpact(response));
    } catch (error) {
      setImpactError(errText(error, "Не удалось загрузить статистику"));
    } finally {
      setImpactLoading(false);
    }
  }
  async function review(order:Order,ratingValue:number,comment:string) {
    try {await api.createReview({orderId:order.id,rating:ratingValue,comment});setDialog(null);await refresh();setToast({type:"success",text:"Отзыв опубликован"});}
    catch(error){setToast({type:"error",text:errText(error,"Не удалось отправить отзыв")});}
  }
  async function saveListing(data:{title:string;description:string;originalPrice:number;foodSavePrice:number;quantity:number;weightKg:number;pickupStart:string;pickupEnd:string;expiresAt:string}) {
    try {if(selected)await api.updateListing(selected.id,data);else await api.createListing(data);
      setSelected(null);setDialog(null);await refresh();setToast({type:"success",text:"Предложение сохранено"});
    }catch(error){setToast({type:"error",text:errText(error,"Не удалось сохранить предложение")});}
  }
  async function cancelListing(listing:Listing){
    if(!window.confirm(`Отменить предложение «${listing.title}»?`))return;
    try{await api.cancelListing(listing.id);await refresh();setToast({type:"success",text:"Предложение отменено"});}
    catch(error){setToast({type:"error",text:errText(error,"Ошибка отмены")});}
  }
  async function saveBusiness(data:{businessName:string;description:string;address:string}){
    try {if(business)await api.updateBusinessProfile(data);else await api.createBusinessProfile(data);
      setDialog(null);await refresh();setToast({type:"success",text:"Профиль бизнеса сохранён"});
    }catch(error){setToast({type:"error",text:errText(error,"Не удалось сохранить профиль")});}
  }
  async function scanQr(token:string){
    try {await api.scanOrderQr(token);setDialog(null);await refresh();setToast({type:"success",text:"Заказ выдан и подтверждён"});}
    catch(error){setToast({type:"error",text:errText(error,"QR не принят")});}
  }
  async function updateProfile(next:Profile){
    const cityChanged=profile?.city!==next.city;
    setProfile(next);setDialog(null);
    if(cityChanged){
      setNearByOff();
      setListings([]); // Не показываем старый город, пока обновляется каталог.
      try {setListings(await api.getListings(next.city));}
      catch(error){setToast({type:"error",text:errText(error,"Не удалось загрузить предложения нового города")});return;}
    }
    setToast({type:"success",text:"Профиль обновлён"});
  }

  if (initializing) return <div className="app-loading"><Leaf size={38}/><b>FoodSave</b><p>Загружаем твой аккаунт…</p></div>;
  if (!authenticated || !profile) return <>
    <Landing onAuth={mode=>{setAuthMode(mode);setAuthOpen(true);}}/>
    {authOpen&&<AuthModal mode={authMode} loading={busy} onClose={()=>setAuthOpen(false)} onMode={setAuthMode} onLogin={login} onRegister={register}/>}
    {toast&&<Toast t={toast}/>}
  </>;
  const isBusiness=profile.role==="BUSINESS";
  const activeRoute=isBusiness?"/business":route==="/orders"?"/orders":"/discover";
  function onNav(event:React.MouseEvent<HTMLAnchorElement>,target:Route){
    if(event.button===0&&!event.metaKey&&!event.ctrlKey&&!event.shiftKey&&!event.altKey){event.preventDefault();navigate(target);}
  }
  return <div className="app-shell">
    <header className="topbar"><div className="container nav">
      <a href={isBusiness?"/business":"/discover"} className="brand" onClick={event=>onNav(event,isBusiness?"/business":"/discover")}><span className="brand-mark"><Leaf size={18}/></span>Food<span>Save</span></a>
      <nav aria-label="Основная навигация">
        {isBusiness?<a className={activeRoute==="/business"?"nav-active":""} href="/business" onClick={event=>onNav(event,"/business")}>Кабинет бизнеса</a>:<>
          <a className={activeRoute==="/discover"?"nav-active":""} href="/discover" onClick={event=>onNav(event,"/discover")}>Предложения</a>
          <a className={activeRoute==="/orders"?"nav-active":""} href="/orders" onClick={event=>onNav(event,"/orders")}>Мои заказы <span className="nav-count">{orders.filter(o=>o.status==="RESERVED").length}</span></a>
        </>}
      </nav>
      <div className="actions"><button className="icon-btn" onClick={()=>void refresh()} title="Обновить данные" disabled={busy}><RefreshCw size={17} className={busy?"spin":""}/></button>
        {isBusiness&&<button className="icon-btn" title="Сканировать QR" onClick={()=>setDialog("scanner")}><ScanLine size={18}/></button>}
        <button className="profile-chip" onClick={()=>setDialog("profile")} title="Мой профиль"><span className="avatar">{profile.username.charAt(0).toUpperCase()}</span><b>{profile.username}</b></button>
        <button className="icon-btn" onClick={logout} title="Выйти"><LogOut size={17}/></button>
      </div>
    </div></header>
    <main>
      {!isBusiness&&activeRoute==="/discover"&&<DiscoverPage city={profile.city} listings={customerListings} hiddenUnknown={hiddenUnknown} nearby={nearby} coords={coords} locating={locating} onLocate={locate} onShowAll={()=>void showAllListings()} onOpen={listing=>{setSelected(listing);setDialog("order")}} onOrders={()=>navigate("/orders")}/>}
      {!isBusiness&&activeRoute==="/orders"&&<OrdersPage profile={profile} orders={orders} paidIds={confirmedPaid} payingId={payingId} onPay={pay} onQr={showOrderQr} onReview={order=>{setSelectedOrder(order);setDialog("review")}} onCancel={cancelOrder} onDiscover={()=>navigate("/discover")} onImpact={loadImpact}/>}
      {isBusiness&&<BusinessPage business={business} listings={myListings} rating={rating} reviewCount={reviews.length} onProfile={()=>setDialog("business")} onCreate={()=>{setSelected(null);setDialog("listing")}} onEdit={listing=>{setSelected(listing);setDialog("listing")}} onCancel={cancelListing} onScanner={()=>setDialog("scanner")}/>}
    </main>
    <footer><div className="container footer"><b>Food<span>Save</span></b><span>Спасение еды — спасение планеты.</span><span>© 2026</span></div></footer>
    {dialog==="profile"&&<ProfileModal profile={profile} onClose={()=>setDialog(null)} onSaved={updateProfile} onPassword={()=>setDialog("password")} onQr={async()=>{try{setQr(await api.getQr());setQrKind("profile");setDialog("qr");}catch(error){setToast({type:"error",text:errText(error,"QR недоступен")});}}}/>}
    {dialog==="password"&&<PasswordModal close={()=>setDialog("profile")} toast={setToast}/>}
    {dialog==="listing"&&isBusiness&&<ListingModal existing={selected} close={()=>{setSelected(null);setDialog(null);}} submit={saveListing}/>}
    {dialog==="order"&&!isBusiness&&selected&&<OrderModal listing={selected} close={()=>setDialog(null)} submit={createOrder}/>}
    {dialog==="qr"&&qr&&<QrModal qr={qr.qrData} kind={qrKind} close={()=>setDialog(null)}/>}
    {dialog==="review"&&selectedOrder&&<ReviewModal order={selectedOrder} close={()=>setDialog(null)} submit={review}/>}
    {dialog==="impact"&&<ImpactModal
      data={impact}
      loading={impactLoading}
      error={impactError}
      completedOrders={profile.completedOrders ?? orders.filter(order=>order.status==="COMPLETED").length}
      foodPoints={profile.foodPoints ?? 0}
      onRetry={()=>void loadImpact()}
      close={()=>setDialog(null)}
    />}
    {dialog==="business"&&isBusiness&&<BusinessModal business={business} close={()=>setDialog(null)} save={saveBusiness}/>}
    {dialog==="scanner"&&isBusiness&&<ScannerModal close={()=>setDialog(null)} scan={scanQr}/>}
    {toast&&<Toast t={toast}/>}
  </div>;
}
