export type Role = "CUSTOMER" | "BUSINESS" | "ADMIN";
export type City = "ALMATY" | "ASTANA";
export type VerificationStatus = "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED";
export type ListingStatus = "ACTIVE" | "SOLD_OUT" | "EXPIRED" | "CANCELED" | "CANCELLED";
export type OrderStatus = "RESERVED" | "COMPLETED" | "CANCELLED" | "EXPIRED" | "NO_SHOW";

export interface AuthResponse { token: string; }

export interface Profile {
  id: string; fsId: string; username: string; email: string; phoneNumber: string;
  city: City; role: Role; verificationStatus: VerificationStatus;
  foodPoints?: number; completedOrders?: number; noShowCount?: number; reliability?: number;
}

export interface FoodSaveQrResponse { fsId: string; qrData: string; }

export interface Listing {
  id: string; title: string; description: string;
  originalPrice: number; foodSavePrice: number; quantity: number; weightKg?: number;
  pickupStart: string; pickupEnd: string; expiresAt: string; status: ListingStatus;
  businessName?: string; businessRating?: number; distanceKm?: number;
  latitude?: number; longitude?: number;
}

export interface Order {
  id: string; fsId: string; listingId: string; listingTitle: string;
  quantity: number; totalPrice: number; status: OrderStatus; createdAt: string;
  expiresAt: string; pickupStart: string; pickupEnd: string;
}

export interface BusinessProfile {
  id: string; userId: string; businessName: string; description?: string;
  address: string; latitude?: number; longitude?: number;
}

export interface Impact { foodSavedKg: number; moneySaved: number; co2SavedKg: number; }
export interface Payment { id: string; orderId: string; amount: number; status: string; createdAt: string; }
export interface OrderQr { qrData: string; }
export interface Review { id: string; orderId: string; businessId: string; username: string; rating: number; comment?: string; createdAt: string; }
