import { useEffect, useRef, useState } from "react";
import { CreditCard, LoaderCircle, ShieldCheck, X } from "lucide-react";
import type { Order } from "../types";
import "./StripeCheckoutModal.css";

type Props = {
  order: Order;
  clientSecret: string;
  onClose: () => void;
};

const money = (value: number, currency = "KZT") =>
    new Intl.NumberFormat("ru-KZ", { style: "currency", currency, maximumFractionDigits: 2 }).format(value);

let checkoutLifecyclePromise: Promise<void> = Promise.resolve();

export function StripeCheckoutModal({ order, clientSecret, onClose }: Props) {
  const checkoutHost = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let disposed = false;
    let checkoutInstance: StripeEmbeddedCheckoutInstance | null = null;

    const init = async () => {
      if (disposed) return;

      const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY as string | undefined;
      if (!key?.trim()) {
        setError("Не задан VITE_STRIPE_PUBLISHABLE_KEY в .env.local фронтенда.");
        setLoading(false);
        return;
      }

      const host = checkoutHost.current;
      if (!window.Stripe || !host) {
        setError("Не удалось загрузить Stripe.js. Проверь подключение к интернету и обнови страницу.");
        setLoading(false);
        return;
      }

      try {
        const stripe = window.Stripe(key);
        const embedded = await stripe.initEmbeddedCheckout({
          fetchClientSecret: async () => clientSecret,
        });

        if (disposed) {
          embedded.destroy();
          return;
        }

        checkoutInstance = embedded;
        embedded.mount(host);
        setLoading(false);
      } catch (cause) {
        if (!disposed) {
          setError(cause instanceof Error ? cause.message : "Не удалось открыть форму Stripe.");
          setLoading(false);
        }
      }
    };

    checkoutLifecyclePromise = checkoutLifecyclePromise.then(init);

    return () => {
      disposed = true;
      checkoutLifecyclePromise = checkoutLifecyclePromise.then(() => {
        if (checkoutInstance) {
          checkoutInstance.destroy();
          checkoutInstance = null;
        }
      });
    };
  }, [clientSecret]);

  return (
      <div className="stripe-pay-backdrop" role="presentation">
        <section className="stripe-pay-modal" role="dialog" aria-modal="true" aria-labelledby="stripe-pay-heading">
          <button className="stripe-pay-close" type="button" onClick={onClose} aria-label="Закрыть оплату">
            <X size={18} />
          </button>
          <div className="stripe-pay-symbol">
            <CreditCard size={25} />
          </div>
          <span className="stripe-pay-eyebrow">FoodSave · Stripe Checkout</span>
          <h2 id="stripe-pay-heading">Безопасная оплата</h2>
          <div className="stripe-pay-summary">
            <span>{order.listingTitle}</span>
            <strong>{money(Number(order.totalPrice))}</strong>
            <small>
              Заказ № {order.id.slice(0, 8)} · {order.quantity} шт.
            </small>
          </div>
          {loading && (
              <div className="stripe-pay-loading">
                <LoaderCircle className="spin" size={20} /> Загружаем защищённую форму оплаты…
              </div>
          )}
          {error && <div className="stripe-pay-error">{error}</div>}
          <div ref={checkoutHost} className="stripe-checkout-host" />
          <p className="stripe-pay-foot">
            <ShieldCheck size={15} /> Данные карты обрабатываются Stripe и не попадают на сервер FoodSave.
          </p>
        </section>
      </div>
  );
}
