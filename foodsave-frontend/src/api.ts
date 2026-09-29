import type { AuthResponse, BusinessProfile, FoodSaveQrResponse, Impact, Listing, Order, OrderQr, Payment, Profile, Review } from "./types";

const API_URL = "http://localhost:8081/api";
function token() { return localStorage.getItem("foodsave_token"); }

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (!(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const jwt = token(); if (jwt) headers.set("Authorization", `Bearer ${jwt}`);
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  const text = await response.text();
  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    try { const data = JSON.parse(text); message = data.message || data.error || text || message; }
    catch { if (text) message = text; }
    throw new Error(message);
  }
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export const api = {
  register: (data: { username:string; email:string; phoneNumber:string; password:string; city:string }) => request<AuthResponse>("/auth/register", {method:"POST",body:JSON.stringify(data)}),
  login: (data: {email:string;password:string}) => request<AuthResponse>("/auth/login", {method:"POST",body:JSON.stringify(data)}),
  getProfile: () => request<Profile>("/profile/me"),
  getQr: () => request<FoodSaveQrResponse>("/profile/qr"),
  updateProfile: (data:{email:string;phoneNumber:string;city:string}) => request<Profile>("/profile/me", {method:"PUT",body:JSON.stringify(data)}),
  changePassword: (data:{oldPassword:string;newPassword:string}) => request<void>("/profile/password", {method:"PUT",body:JSON.stringify(data)}),

  getListings: () => request<Listing[]>("/listings"),
  getNearbyListings: (lat:number, lon:number, radius=10) => request<Listing[]>(`/listings/nearby?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}&radius=${radius}`),
  getMyListings: () => request<Listing[]>("/listings/my"),
  createListing: (data:{title:string;description:string;originalPrice:number;foodSavePrice:number;quantity:number;pickupStart:string;pickupEnd:string;expiresAt:string;weightKg:number}) => request<Listing>("/listings", {method:"POST",body:JSON.stringify(data)}),
  updateListing: (id:string,data:Partial<{title:string;description:string;originalPrice:number;foodSavePrice:number;quantity:number;pickupStart:string;pickupEnd:string;expiresAt:string;weightKg:number}>) => request<Listing>(`/listings/${id}`, {method:"PUT",body:JSON.stringify(data)}),
  cancelListing: (id:string) => request<void>(`/listings/${id}`, {method:"DELETE"}),

  getBusinessProfile: () => request<BusinessProfile>("/business/profile"),
  createBusinessProfile: (data:{businessName:string;description:string;address:string}) => request<BusinessProfile>("/business/profile", {method:"POST",body:JSON.stringify(data)}),
  updateBusinessProfile: (data:{businessName:string;description:string;address:string}) => request<BusinessProfile>("/business/profile", {method:"PUT",body:JSON.stringify(data)}),

  createOrder: (listingId:string, quantity:number) => request<Order>("/orders", {method:"POST",body:JSON.stringify({listingId,quantity})}),
  getMyOrders: () => request<Order[]>("/orders/my"),
  cancelOrder: (id:string) => request<void>(`/orders/${id}`, {method:"DELETE"}),
  payForOrder: (orderId:string) => request<Payment>(`/payments/${orderId}`, {method:"POST"}),
  getOrderQr: (id:string) => request<OrderQr>(`/orders/${id}/qr`),
  scanOrderQr: (qrToken:string) => request<Order>("/orders/scan", {method:"POST",body:JSON.stringify({qrToken})}),

  getImpact: () => request<Impact>("/impact/my"),
  createReview: (data:{orderId:string;rating:number;comment:string}) => request<Review>("/reviews", {method:"POST",body:JSON.stringify(data)}),
  getMyReviews: () => request<Review[]>("/reviews/my"),
  getMyRating: () => request<number>("/reviews/my/rating")
};
