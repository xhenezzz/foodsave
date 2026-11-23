import { useEffect, useRef, useState } from "react";
import { ArrowLeft, ArrowRight, CheckCircle2, Clock3, Leaf, LoaderCircle, MailCheck, RefreshCw, ShieldCheck, TriangleAlert } from "lucide-react";
import { errText } from "../utils";

type EmailView = "check" | "verify";
type VerifyStatus = "loading" | "success" | "error";

interface Props {
  view: EmailView;
  email: string;
  rawToken: string | null;
  onVerify: (rawToken: string) => Promise<void>;
  onResend: (email: string) => Promise<void>;
  onLogin: () => void;
  onHome: () => void;
}

export function EmailVerificationPage({view, email, rawToken, onVerify, onResend, onLogin, onHome}: Props) {
  const [verification, setVerification] = useState<VerifyStatus>("loading");
  const [verificationError, setVerificationError] = useState("");
  const [resending, setResending] = useState(false);
  const [resendMessage, setResendMessage] = useState<{ok:boolean;text:string} | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (view !== "verify" || started.current) return;
    started.current = true;
    if (!rawToken) {
      setVerification("error");
      setVerificationError("В ссылке отсутствует токен подтверждения. Открой ссылку из письма FoodSave.");
      return;
    }
    window.history.replaceState({}, "", "/verify-email");
    void onVerify(rawToken).then(
      () => setVerification("success"),
      error => {
        setVerification("error");
        setVerificationError(errText(error, "Не получилось подтвердить email. Проверь ссылку или запроси новое письмо."));
      }
    );
  }, [view, rawToken, onVerify]);

  async function resend() {
    if (!email || resending) return;
    setResending(true);
    setResendMessage(null);
    try {
      await onResend(email);
      setResendMessage({ok:true,text:"Если аккаунт ожидает подтверждения, новое письмо будет отправлено. Проверь также папку «Спам»."});
    } catch (error) {
      setResendMessage({ok:false,text:errText(error, "Не удалось отправить письмо повторно")});
    } finally {
      setResending(false);
    }
  }

  const success = view === "verify" && verification === "success";
  const error = view === "verify" && verification === "error";
  const loading = view === "verify" && verification === "loading";

  return <div className="verify-shell">
    <header className="verify-topbar">
      <a href="/" className="verify-brand" onClick={event=>{event.preventDefault();onHome();}}>
        <span className="verify-brand-icon"><Leaf size={20}/></span> Food<span>Save</span>
      </a>
      <span className="verify-header-hint"><ShieldCheck size={15}/> Безопасная регистрация</span>
    </header>

    <main className="verify-main">
      <div className="verify-decoration verify-decoration-left" aria-hidden="true">✳</div>
      <div className="verify-decoration verify-decoration-right" aria-hidden="true">✳</div>
      <div className="verify-card">
        <div className={`verify-symbol ${success ? "verify-symbol-success" : error ? "verify-symbol-error" : ""}`}>
          {success ? <CheckCircle2 size={38} strokeWidth={1.7}/> : error ? <TriangleAlert size={38} strokeWidth={1.7}/> : loading ? <LoaderCircle className="spin" size={38} strokeWidth={1.7}/> : <MailCheck size={38} strokeWidth={1.7}/>}
        </div>
        <span className="verify-eyebrow">FOODSAVE · EMAIL VERIFICATION</span>
        <h1>{success ? "Почта подтверждена!" : error ? "Не удалось подтвердить" : loading ? "Подтверждаем почту…" : "Проверь свою почту"}</h1>
        <p className="verify-description">
          {success ? "Отлично! Почта подтверждена, и ты автоматически вошёл в FoodSave." :
           error ? verificationError :
           loading ? "Проверяем ссылку из письма. Обычно это занимает несколько секунд." :
           "Мы отправили письмо со ссылкой для подтверждения аккаунта на адрес:"}
        </p>
        {view === "check" && email && <div className="verify-email"><MailCheck size={18}/><strong>{email}</strong></div>}
        {view === "check" && <div className="verify-steps">
          <div><span className="verify-step-no">01</span><span>Открой письмо от FoodSave</span><CheckCircle2 size={17}/></div>
          <div><span className="verify-step-no">02</span><span>Нажми «Verify Email»</span><ArrowRight size={17}/></div>
          <div><span className="verify-step-no">03</span><span>После подтверждения ты войдёшь автоматически</span><ArrowRight size={17}/></div>
        </div>}
        {(view === "check" || error) && <div className="verify-info"><Clock3 size={18}/><span>Ссылка действительна <b>30 минут</b>. Если письма нет, проверь папку «Спам».</span></div>}
        {resendMessage && <div role="status" className={`verify-feedback ${resendMessage.ok ? "ok" : "bad"}`}>{resendMessage.text}</div>}
        {view === "check" && <button className="primary-btn full verify-main-action" onClick={onLogin}>Уже подтвердил? Войти <ArrowRight size={17}/></button>}
        {success && <button className="primary-btn full verify-main-action" onClick={onHome}>Открыть FoodSave <ArrowRight size={17}/></button>}
        {(view === "check" || error) && <button className="verify-resend" onClick={()=>void resend()} disabled={!email || resending}>
          <RefreshCw size={16} className={resending ? "spin" : ""}/>{resending ? "Отправляем…" : "Отправить письмо повторно"}
        </button>}
        {error && !email && <p className="verify-tip">Для повторной отправки открой экран после регистрации или используй свой email в форме входа, когда backend поддержит эту функцию.</p>}
        <button className="verify-home" onClick={onHome}><ArrowLeft size={16}/> На главную</button>
      </div>
      <div className="verify-benefits"><Leaf size={16}/> Спасай еду <span>·</span> Экономь деньги <span>·</span> Заботься о планете</div>
    </main>
  </div>;
}
