import type {
  AuthResponse,
  BusinessProfile,
  City,
  FoodSaveQrResponse,
  Impact,
  Listing,
  Loyalty,
  BusinessDashboard,
  AiListingSuggestion,
  AiListingDraft,
  FoodCategory,
  ListingImage,
  Order,
  OrderQr,
  Payment,
  StripeCheckoutSession,
  Profile,
  Review
} from "./types";

const API_URL = (import.meta.env?.VITE_API_URL || "http://localhost:8081/api").replace(/\/$/, "");

function token(): string | null {
  return localStorage.getItem("foodsave_token");
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {

  const headers = new Headers(options.headers);

  if (!(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const jwt = token();

  if (jwt) {
    headers.set("Authorization", `Bearer ${jwt}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers
  });

  const text = await response.text();

  if (!response.ok) {

    let message = `HTTP ${response.status}`;

    try {
      const data = JSON.parse(text);
      message =
        data.message ||
        data.error ||
        text ||
        message;
    } catch {
      if (text) {
        message = text;
      }
    }

    throw new Error(message);
  }

  if (!text) {
    return undefined as T;
  }

  try { return JSON.parse(text) as T; }
  catch { return text as T; }
}


/* =========================================================
   AUTH
   ========================================================= */

export const api = {
  getListingImages: (id:string) => request<ListingImage[]>(`/listings/${id}/images`),
  uploadListingImage: (id:string, file:File) => {
    const data=new FormData(); data.append("file",file);
    return request<ListingImage>(`/listings/${id}/images`,{method:"POST",body:data});
  },
  deleteListingImage: (id:string, imageId:string) => request<void>(`/listings/${id}/images/${imageId}`,{method:"DELETE"}),
  getListingImageBlob: async (id:string, imageId:string): Promise<Blob> => {
    const jwt=token();
    const response=await fetch(`${API_URL}/listings/${id}/images/${imageId}/content`,{
      headers:jwt?{Authorization:`Bearer ${jwt}`}:{},
    });
    if(!response.ok) throw new Error(`Не удалось загрузить фото: HTTP ${response.status}`);
    return response.blob();
  },

  suggestAiListing: (data: {productName:string; details:string; language:"ru"|"kk"|"en"}) =>
    request<AiListingSuggestion>("/business/ai-listing/suggest", {
      method: "POST", body: JSON.stringify(data)
    }),

  analyzeAiListing: (image:File, language:"ru"|"kk"|"en", notes:string) => {
    const data = new FormData();
    data.append("image", image);
    data.append("language", language);
    if (notes.trim()) data.append("notes", notes.trim());
    return request<AiListingDraft>("/business/ai-listing/analyze", {method:"POST", body:data});
  },


  register: (data: {
    username: string;
    email: string;
    phoneNumber: string;
    password: string;
    city: City;
    role: "CUSTOMER" | "BUSINESS";
  }) =>
    request<{token?:string;message?:string}>("/auth/register", {
      method: "POST",
      body: JSON.stringify(data)
    }),

  login: (data: {
    email: string;
    password: string;
  }) =>
    request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(data)
    }),


  verifyEmail: (rawToken: string) =>
    request<AuthResponse>(`/auth/verify-email?token=${encodeURIComponent(rawToken)}`, {method: "POST"}),

  resendVerification: (email: string) =>
    request<void>("/auth/resend-verification", {
      method: "POST",
      body: JSON.stringify({email})
    }),

  /* =========================================================
     PROFILE
     ========================================================= */

  getProfile: () =>
    request<Profile>("/profile/me"),

  getBusinessStatistics: (days: 7 | 30 | 90 = 30) =>
    request<BusinessDashboard>(`/business/statistics?days=${days}`),

  getLoyalty: () =>
    request<Loyalty>("/loyalty/me"),


  getQr: () =>
    request<FoodSaveQrResponse>("/profile/qr"),


  updateProfile: (data: {
    email: string;
    phoneNumber: string;
    city: City;
  }) =>
    request<Profile>("/profile/me", {
      method: "PUT",
      body: JSON.stringify(data)
    }),


  changePassword: (data: {
    oldPassword: string;
    newPassword: string;
  }) =>
    request<void>("/profile/password", {
      method: "PUT",
      body: JSON.stringify(data)
    }),


  /* =========================================================
     FOOD LISTINGS
     ========================================================= */

  getListings: (city?: City) =>
    request<Listing[]>(city ? `/listings?city=${encodeURIComponent(city)}` : "/listings"),


  getNearbyListings: (
    lat: number,
    lon: number,
    radius = 10,
    city?: City
  ) =>
    request<Listing[]>(
      `/listings/nearby?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}&radius=${encodeURIComponent(radius)}${city ? `&city=${encodeURIComponent(city)}` : ""}`
    ),


  getListing: (id: string) =>
    request<Listing>(`/listings/${id}`),


  getMyListings: () =>
    request<Listing[]>("/listings/my"),


  createListing: (data: {
    title: string;
    description: string;
    originalPrice: number;
    foodSavePrice: number;
    quantity: number;
    pickupStart: string;
    pickupEnd: string;
    expiresAt: string;
    productExpiresAt: string;
    weightKg: number;
    category: FoodCategory;
  }) =>
    request<Listing>("/listings", {
      method: "POST",
      body: JSON.stringify(data)
    }),


  updateListing: (
    id: string,
    data: {
      version: number;
      title: string;
      description: string;
      originalPrice: number;
      foodSavePrice: number;
      quantity: number;
      pickupStart: string;
      pickupEnd: string;
      expiresAt: string;
    productExpiresAt: string;
      weightKg: number;
      category: FoodCategory;
    }
  ) =>
    request<Listing>(`/listings/${id}`, {
      method: "PUT",
      body: JSON.stringify(data)
    }),


  cancelListing: (id: string) =>
    request<void>(`/listings/${id}`, {
      method: "DELETE"
    }),


  /* =========================================================
     BUSINESS PROFILE
     ========================================================= */

  getBusinessProfile: () =>
    request<BusinessProfile>("/business/profile"),


  createBusinessProfile: (data: {
    businessName: string;
    description: string;
    address: string;
    latitude: number;
    longitude: number;
  }) =>
    request<BusinessProfile>("/business/profile", {
      method: "POST",
      body: JSON.stringify(data)
    }),


  updateBusinessProfile: (data: {
    businessName: string;
    description: string;
    address: string;
    latitude: number;
    longitude: number;
  }) =>
    request<BusinessProfile>("/business/profile", {
      method: "PUT",
      body: JSON.stringify(data)
    }),


  /* =========================================================
     ORDERS
     ========================================================= */

  createOrder: (
    listingId: string,
    quantity: number
  ) =>
    request<Order>("/orders", {
      method: "POST",
      body: JSON.stringify({
        listingId,
        quantity
      })
    }),


  getMyOrders: () =>
    request<Order[]>("/orders/my"),


  getOrder: (id: string) =>
    request<Order>(`/orders/${id}`),


  cancelOrder: (id: string) =>
    request<void>(`/orders/${id}`, {
      method: "DELETE"
    }),


  getOrderQr: (id: string) =>
    request<OrderQr>(`/orders/${id}/qr`),


  scanOrderQr: (qrToken: string) =>
    request<Order>("/orders/scan", {
      method: "POST",
      body: JSON.stringify({
        qrToken
      })
    }),


  /* =========================================================
     PAYMENT
     ========================================================= */

  // Legacy demo endpoints are retained for compatibility with existing local tests/data.
  // The customer-facing app now uses Stripe Embedded Checkout below.
  startDemoPayment: (orderId: string) =>
    request<Payment>(`/payments/${orderId}/demo/start`, {method:"POST"}),
  confirmDemoPayment: (orderId: string, decision: "APPROVE" | "DECLINE") =>
    request<Payment>(`/payments/${orderId}/demo/confirm`, {
      method:"POST", body:JSON.stringify({decision})
    }),
  startStripeCheckout: (orderId: string) =>
    request<StripeCheckoutSession>(`/payments/${orderId}/stripe/checkout-session`, {method:"POST"}),
  getPaymentStatus: (orderId: string) =>
    request<Payment>(`/payments/${orderId}`),


  /* =========================================================
     IMPACT
     ========================================================= */

  getImpact: () =>
    request<Impact>("/impact/me"),


  /* =========================================================
     REVIEWS
     ========================================================= */

  createReview: (data: {
    orderId: string;
    rating: number;
    comment: string;
  }) =>
    request<Review>("/reviews", {
      method: "POST",
      body: JSON.stringify(data)
    }),


  getMyReviews: () =>
    request<Review[]>("/reviews/my"),


  getMyRating: () =>
    request<number>("/reviews/my/rating")
};