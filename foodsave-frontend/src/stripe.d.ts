export {};

declare global {
  interface StripeEmbeddedCheckoutInstance {
    mount(target: string | HTMLElement): void;
    unmount(): void;
    destroy(): void;
  }

  interface StripeInstance {
    initEmbeddedCheckout(options: { fetchClientSecret: () => Promise<string> }): Promise<StripeEmbeddedCheckoutInstance>;
  }

  interface Window {
    Stripe?: (publishableKey: string) => StripeInstance;
  }
}
