import { useEffect, useMemo, useState } from "react";
import { Leaf, LogOut, RefreshCw, ScanLine } from "lucide-react";
import { api } from "./api";
import { parseImpact } from "./impact";
import type { BusinessProfile, City, FoodCategory, FoodSaveQrResponse, Impact, Listing, Loyalty, Order, Profile, Review } from "./types";
import { errText, confirmedPayment, coordinatesCity, listingsForCity, listingAvailable, listingCity, orderPaidByServer, cityName } from "./utils";
import { AuthModal, BusinessModal, ImpactModal, Landing, ListingModal, OrderModal, PasswordModal, ProfileModal, QrModal, ReviewModal, ScannerModal, Toast } from "./components";
import { DiscoverPage } from "./pages/DiscoverPage";
import { OrdersPage } from "./pages/OrdersPage";
import { BusinessPage } from "./pages/BusinessPage";
import { EmailVerificationPage } from "./pages/EmailVerificationPage";
import { ReputationModal } from "./pages/ReputationModal";
import { StripeCheckoutModal } from "./pages/StripeCheckoutModal";
import type { StripeCheckoutSession } from "./types";

type Route = "/discover" | "/orders" | "/business";
type EmailFlow = "check" | "verify" | null;
function readEmailFlow(): EmailFlow {
  if (location.pathname === "/check-email") return "check";
  if (location.pathname === "/verify-email") return "verify";
  return null;
}
type Dialog = "profile" | "password" | "listing" | "business" | "order" | "qr" | "review" | "scanner" | "impact" | "reputation" | "stripePayment" | null;
type ToastValue = {type: "success" | "error"; text: string} | null;
const roleHome = (profile: Profile): Route => profile.role === "BUSINESS" ? "/business" : "/discover";
function readRoute(): Route | null {
  if (location.pathname === "/discover") return "/discover";
  if (location.pathname === "/orders") return "/orders";
  if (location.pathname === "/business") return "/business";
  return null;
}

export default function App() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [emailFlow, setEmailFlow] = useState<EmailFlow>(readEmailFlow);
  const [verificationToken] = useState<string | null>(() => new URLSearchParams(location.search).get("token"));
  const [pendingEmail, setPendingEmail] = useState(() => localStorage.getItem("foodsave_pending_email") || "");
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
  const [reputation, setReputation] = useState<Loyalty | null>(null);
  const [reputationLoading, setReputationLoading] = useState(false);
  const [reputationError, setReputationError] = useState<string | null>(null);
  const [impactLoading, setImpactLoading] = useState(false);
  const [impactError, setImpactError] = useState<string | null>(null);
  const [dialog, setDialog] = useState<Dialog>(null);
  const [selected, setSelected] = useState<Listing | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [qr, setQr] = useState<FoodSaveQrResponse | null>(null);
  const [toast, setToast] = useState<ToastValue>(null);
  const [busy, setBusy] = useState(false);
  const [locating, setLocating] = useState(false);
  const [payingId, setPayingId] = useState<string | null>(null);
  const [stripeCheckout, setStripeCheckout] = useState<StripeCheckoutSession | null>(null);
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
    const handlePop = () => { setRoute(readRoute()); setEmailFlow(readEmailFlow()); };
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
    if (!profile || emailFlow) return;
    const target = roleHome(profile);
    const validRoute = profile.role === "BUSINESS" ? route === "/business" : route === "/discover" || route === "/orders";
    if (!validRoute) navigate(target, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile?.role, route, emailFlow]);

  async function refresh(firstLoad = false) {
    setBusy(true);
    try {
      const person = await api.getProfile();
      // Defense in depth for older JWTs, provided the backend exposes emailVerified.
      // Actual authorization must still be enforced by Spring Security.
      if (person.emailVerified === false || (person.emailVerified == null && localStorage.getItem("foodsave_pending_email")?.toLowerCase() === person.email.toLowerCase())) {
        localStorage.removeItem("foodsave_token");
        localStorage.setItem("foodsave_pending_email", person.email);
        setPendingEmail(person.email);
        setAuthenticated(false);
        setProfile(null);
        setEmailFlow("check");
        history.replaceState({}, "", "/check-email");
        return;
      }
      setProfile(person);
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

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("payment") !== "return") return;
    const paymentOrderId = params.get("orderId") ?? "";
    if (!paymentOrderId) {
      history.replaceState({}, "", "/orders");
      return;
    }
    let cancelled = false;
    const delay = (ms:number) => new Promise(resolve => window.setTimeout(resolve, ms));
    async function waitForStripeWebhook() {
      for (let attempt = 0; attempt < 15 && !cancelled; attempt++) {
        try {
          const payment = await api.getPaymentStatus(paymentOrderId);
          if (payment.status === "SUCCESS") {
            await refresh();
            if (!cancelled) setToast({type:"success",text:"Оплата подтверждена Stripe. QR-код доступен в заказе."});
            history.replaceState({}, "", "/orders");
            return;
          }
          if (payment.status === "FAILED") {
            await refresh();
            if (!cancelled) setToast({type:"error",text:"Платёж не прошёл или бронь истекла. Можно попробовать снова."});
            history.replaceState({}, "", "/orders");
            return;
          }
        } catch {
          // Webhook may still be in flight. Retry instead of trusting the browser redirect.
        }
        await delay(1000);
      }
      if (!cancelled) {
        await refresh();
        setToast({type:"success",text:"Stripe вернул тебя в FoodSave. Проверка платежа ещё идёт — обнови заказы чуть позже."});
        history.replaceState({}, "", "/orders");
      }
    }
    void waitForStripeWebhook();
    return () => { cancelled = true; };
    // The return URL is processed once after Stripe redirects back to the app.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function login(email:string,password:string) {
    setBusy(true);
    try {
      const result = await api.login({email,password});
      if (!result?.token) throw new Error("Сервер не вернул JWT после входа");
      localStorage.setItem("foodsave_token",result.token);
      const person = await api.getProfile();
      if (person.emailVerified === false || (person.emailVerified == null && localStorage.getItem("foodsave_pending_email")?.toLowerCase() === person.email.toLowerCase())) {
        localStorage.removeItem("foodsave_token");
        localStorage.setItem("foodsave_pending_email", person.email);
        setPendingEmail(person.email);
        setAuthenticated(false);
        setProfile(null);
        setAuthOpen(false);
        setEmailFlow("check");
        history.replaceState({}, "", "/check-email");
        return;
      }
      setEmailFlow(null);
      setAuthenticated(true);
      setAuthOpen(false);
      await refresh();
      setToast({type:"success",text:"Добро пожаловать в FoodSave"});
    } catch(error) {
      // A failed login should never leave a freshly obtained, unvalidated JWT in storage.
      localStorage.removeItem("foodsave_token");
      setToast({type:"error",text:errText(error,"Не удалось войти")});
    } finally {setBusy(false);}
  }
  async function register(data:{username:string;email:string;phoneNumber:string;password:string;city:City;role:"CUSTOMER"|"BUSINESS"}) {
    setBusy(true);
    try {
      // Older backends return a JWT immediately; deliberately do not use it here.
      await api.register(data);
      localStorage.removeItem("foodsave_token");
      localStorage.setItem("foodsave_pending_email", data.email);
      setPendingEmail(data.email);
      setAuthenticated(false);
      setProfile(null);
      setAuthOpen(false);
      setEmailFlow("check");
      history.pushState({}, "", "/check-email");
      setToast({type:"success",text:"Аккаунт создан. Проверь письмо для подтверждения email."});
    } catch(error) {setToast({type:"error",text:errText(error,"Не удалось зарегистрироваться")});}
    finally {setBusy(false);}
  }
  async function verifyEmail(rawToken: string) {
    const result = await api.verifyEmail(rawToken);
    if (!result?.token) {
      throw new Error("Сервер подтвердил почту, но не вернул JWT для входа");
    }

    // Reuse the exact same token storage and profile bootstrap as the regular login flow.
    localStorage.setItem("foodsave_token", result.token);

    try {
      // Validate the newly issued JWT before switching the UI into authenticated mode.
      const person = await api.getProfile();
      if (person.emailVerified === false) {
        throw new Error("Почта ещё не подтверждена. Попробуй открыть ссылку повторно.");
      }

      localStorage.removeItem("foodsave_pending_email");
      setPendingEmail("");
      setProfile(person);
      setAuthenticated(true);
      setAuthOpen(false);
      setEmailFlow(null);
      setRoute(null);
      history.replaceState({}, "", "/");

      // Load the rest of the user's workspace (orders/listings/business profile).
      void refresh();
      setToast({type:"success",text:"Почта подтверждена. Ты автоматически вошёл в FoodSave."});
    } catch (error) {
      // Do not keep a token that failed the profile check.
      localStorage.removeItem("foodsave_token");
      setAuthenticated(false);
      setProfile(null);
      throw error;
    }
  }
  async function resendVerification(email: string) {
    await api.resendVerification(email);
  }
  function openLogin() {
    setEmailFlow(null);
    history.replaceState({}, "", "/");
    setRoute(null);
    setAuthMode("login");
    setAuthOpen(true);
  }
  function openHome() {
    setEmailFlow(null);
    history.replaceState({}, "", "/");
    setRoute(null);
    setAuthOpen(false);
  }
  function logout() {
    localStorage.removeItem("foodsave_token");
    setAuthenticated(false);setProfile(null);setListings([]);setOrders([]);setMyListings([]);setBusiness(null);
    setStripeCheckout(null);setQr(null);setDialog(null);setImpact(null);setImpactError(null);setNearByOff();
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
  const confirmedPaid = useMemo(() => new Set(orders.filter(orderPaidByServer).map(order=>order.id)),[orders]);

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
      const session = await api.startStripeCheckout(order.id);
      if (confirmedPayment(session.status)) {
        await refresh();
        setToast({type:"success",text:"Заказ уже оплачен. QR доступен в заказе."});
        return;
      }
      if (session.status !== "PENDING" || !session.clientSecret) {
        throw new Error("Stripe не вернул активную платёжную сессию");
      }
      setSelectedOrder(order);
      setStripeCheckout(session);
      setDialog("stripePayment");
    } catch(error) {setToast({type:"error",text:errText(error,"Не удалось открыть Stripe Checkout")});}
    finally {setPayingId(null);}
  }
  async function showOrderQr(order:Order){
    try {const orderQr=await api.getOrderQr(order.id);setQr({fsId:order.fsId,qrData:orderQr.qrData});setSelectedOrder(order);setDialog("qr");}
    catch(error){setToast({type:"error",text:errText(error,"QR пока недоступен")});}
  }
  async function cancelOrder(order:Order){
    if (!window.confirm("Отменить бронирование?")) return;
    try {await api.cancelOrder(order.id);await refresh();setToast({type:"success",text:"Заказ отменён"});}
    catch(error){setToast({type:"error",text:errText(error,"Не удалось отменить")});}
  }
  async function loadReputation() {
    setDialog("reputation");
    setReputationLoading(true);
    setReputationError(null);
    try { setReputation(await api.getLoyalty()); }
    catch (error) { setReputationError(errText(error, "Не удалось загрузить репутацию")); }
    finally { setReputationLoading(false); }
  }
  async function loadImpact(){
    // Открываем окно сразу: ошибка API больше не выглядит как «нерабочая кнопка».
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
  async function saveListing(data:{title:string;description:string;originalPrice:number;foodSavePrice:number;quantity:number;weightKg:number;pickupStart:string;pickupEnd:string;expiresAt:string;productExpiresAt:string;category:FoodCategory},photos:File[]) {
    try {
      const saved=selected?await api.updateListing(selected.id,{...data,version:selected.version ?? -1}):await api.createListing(data);
      const listingId=selected?.id??saved.id;
      let uploadsFailed=0;
      for(const file of photos){try{await api.uploadListingImage(listingId,file);}catch{uploadsFailed++;}}
      setSelected(null);setDialog(null);await refresh();
      setToast(uploadsFailed?{type:"error",text:`Объявление сохранено, но ${uploadsFailed} фото не загрузились. Открой редактирование и повтори.`}:{type:"success",text:"Предложение и фотографии сохранены"});
    }catch(error){setToast({type:"error",text:errText(error,"Не удалось сохранить предложение")});}
  }
  async function cancelListing(listing:Listing){
    if(!window.confirm(`Отменить предложение «${listing.title}»?`))return;
    try{await api.cancelListing(listing.id);await refresh();setToast({type:"success",text:"Предложение отменено"});}
    catch(error){setToast({type:"error",text:errText(error,"Ошибка отмены")});}
  }
  async function saveBusiness(data:{businessName:string;description:string;address:string;latitude:number;longitude:number}){
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

  if (emailFlow) return <>
    <EmailVerificationPage
      view={emailFlow}
      email={pendingEmail}
      rawToken={verificationToken}
      onVerify={verifyEmail}
      onResend={resendVerification}
      onLogin={openLogin}
      onHome={openHome}
    />
    {toast && <Toast t={toast}/>}
  </>;
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
      <div className="actions"><button className="icon-btn" onClick={()=>void refresh()} title="Обновить данные" aria-label="Обновить данные" disabled={busy}><RefreshCw size={17} className={busy?"spin":""}/></button>
        {isBusiness&&<button className="icon-btn" title="Сканировать QR" aria-label="Сканировать QR" onClick={()=>setDialog("scanner")}><ScanLine size={18}/></button>}
        <button className="profile-chip" onClick={()=>setDialog("profile")} title="Мой профиль" aria-label="Открыть мой профиль"><span className="avatar">{profile.username.charAt(0).toUpperCase()}</span><b>{profile.username}</b></button>
        <button className="icon-btn" onClick={logout} title="Выйти" aria-label="Выйти из аккаунта"><LogOut size={17}/></button>
      </div>
    </div></header>
    <main>
      {!isBusiness&&activeRoute==="/discover"&&<DiscoverPage city={profile.city} listings={customerListings} hiddenUnknown={hiddenUnknown} nearby={nearby} coords={coords} locating={locating} onLocate={locate} onShowAll={()=>void showAllListings()} onOpen={listing=>{setSelected(listing);setDialog("order")}} onOrders={()=>navigate("/orders")}/>}
      {!isBusiness&&activeRoute==="/orders"&&<OrdersPage profile={profile} orders={orders} paidIds={confirmedPaid} payingId={payingId} onPay={pay} onQr={showOrderQr} onReview={order=>{setSelectedOrder(order);setDialog("review")}} onCancel={cancelOrder} onDiscover={()=>navigate("/discover")} onImpact={loadImpact}/>}
      {isBusiness&&<BusinessPage business={business} listings={myListings} rating={rating} reviewCount={reviews.length} onProfile={()=>setDialog("business")} onCreate={()=>{setSelected(null);setDialog("listing")}} onEdit={listing=>{setSelected(listing);setDialog("listing")}} onCancel={cancelListing} onScanner={()=>setDialog("scanner")}/>}
    </main>
    <footer><div className="container footer"><b>Food<span>Save</span></b><span>Спасение еды — спасение планеты.</span><span>© 2026</span></div></footer>
    {dialog==="profile"&&<ProfileModal profile={profile} onClose={()=>setDialog(null)} onSaved={updateProfile} onPassword={()=>setDialog("password")} onReputation={()=>void loadReputation()}/>}
    {dialog==="reputation"&&<ReputationModal data={reputation} loading={reputationLoading} error={reputationError} onRetry={()=>void loadReputation()} onClose={()=>setDialog("profile")}/>}
    {dialog==="password"&&<PasswordModal close={()=>setDialog("profile")} toast={setToast}/>}
    {dialog==="listing"&&isBusiness&&<ListingModal existing={selected} close={()=>{setSelected(null);setDialog(null);}} submit={saveListing}/>}
    {dialog==="order"&&!isBusiness&&selected&&<OrderModal listing={selected} close={()=>setDialog(null)} submit={createOrder}/>}
    {dialog==="qr"&&qr&&<QrModal qr={qr.qrData} close={()=>setDialog(null)}/>}
    {dialog==="stripePayment"&&selectedOrder&&stripeCheckout?.clientSecret&&<StripeCheckoutModal order={selectedOrder} clientSecret={stripeCheckout.clientSecret} onClose={()=>{setDialog(null);setStripeCheckout(null);}}/>}
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
    {dialog==="business"&&isBusiness&&<BusinessModal business={business} city={profile.city} close={()=>setDialog(null)} save={saveBusiness}/>}
    {dialog==="scanner"&&isBusiness&&<ScannerModal close={()=>setDialog(null)} scan={scanQr}/>}
    {toast&&<Toast t={toast}/>}
  </div>;
}
