export type Role = "CUSTOMER" | "BUSINESS" | "ADMIN";
export type City = "ALMATY" | "ASTANA";
export type VerificationStatus = "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED";
export type ListingStatus = "ACTIVE" | "SOLD_OUT" | "EXPIRED" | "CANCELED" | "CANCELLED";
export type OrderStatus = "RESERVED" | "COMPLETED" | "CANCELLED" | "EXPIRED" | "NO_SHOW";
export type FoodCategory = "BAKERY" | "READY_MEALS" | "DESSERTS" | "DAIRY" | "FRUITS_VEGETABLES" | "MEAT_FISH" | "GROCERIES" | "DRINKS" | "OTHER";

export type ModerationStatus = "PENDING" | "APPROVED" | "REJECTED";
export type BusinessType = "BAKERY" | "CAFE" | "RESTAURANT" | "COFFEE_SHOP" | "GROCERY" | "SUPERMARKET" | "CONFECTIONERY" | "OTHER";

/** Nutrition facts per 100 g; every value is optional. */
export interface Nutrition { calories?: number | null; proteins?: number | null; fats?: number | null; carbs?: number | null; }

/** Data the business submits from the listing form. */
export type ListingFormData = {
  title: string; description: string; originalPrice: number; foodSavePrice: number; quantity: number; weightKg: number;
  pickupStart: string; pickupEnd: string; expiresAt: string; productExpiresAt: string; category: FoodCategory;
} & Nutrition;

export interface AuthResponse { token: string; }

export interface Profile {
  id: string; fsId: string; username: string; email: string; phoneNumber: string;
  city: City; role: Role; verificationStatus: VerificationStatus; emailVerified?: boolean;
  foodPoints?: number; completedOrders?: number; noShowCount?: number; reliability?: number;
}

export type ReputationLevel = "NEW" | "BRONZE" | "SILVER" | "GOLD" | "NEEDS_IMPROVEMENT";
export interface Loyalty {
  fsId: string;
  foodPoints: number;
  totalOrders: number;
  completedOrders: number;
  noShowCount: number;
  reliabilityPercent: number | null;
  reputationLevel: ReputationLevel;
  canCreateOrder: boolean;
  bookingBlockedUntil: string | null;
  noShowsUntilNextBlock: number;
}

export interface FoodSaveQrResponse { fsId: string; qrData: string; }

export interface Listing extends Nutrition {
  id: string; title: string; description: string; category?: FoodCategory | null;
  originalPrice: number; foodSavePrice: number; quantity: number; weightKg?: number; version?: number;
  pickupStart: string; pickupEnd: string; expiresAt: string; productExpiresAt?:string|null; status: ListingStatus;
  businessName?: string; businessRating?: number; distanceKm?: number; recommendationScore?: number | null;
  city?: City; businessCity?: City; ownerId?: string;
  latitude?: number; longitude?: number;
  moderationStatus?: ModerationStatus; moderationReason?: string | null;
}

export interface Order {
  id: string; fsId: string; listingId: string; listingTitle: string;
  quantity: number; totalPrice: number; status: OrderStatus; createdAt: string;
  expiresAt: string; pickupStart: string; pickupEnd: string;
  paid?: boolean; paymentStatus?: string;
}

export interface BusinessProfile {
  id: string; userId: string; businessName: string; description?: string | null;
  address: string; latitude?: number | null; longitude?: number | null;
  businessType?: BusinessType | null; contactPhone?: string | null; bin?: string | null;
  status: ModerationStatus; rejectionReason?: string | null; submittedAt?: string | null; reviewedAt?: string | null;
}

/** Business application form (create and edit). */
export interface BusinessApplication {
  businessName: string; businessType: BusinessType; description: string; address: string;
  latitude: number; longitude: number; contactPhone: string; bin: string;
}

export interface Impact { foodSavedKg: number; moneySaved: number; co2SavedKg: number; }
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED";
export interface Payment { id: string; orderId: string; amount: number; status: PaymentStatus; createdAt: string; }
export interface StripeCheckoutSession {
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  clientSecret: string | null;
}
export interface OrderQr { qrData: string; }
export interface Review { id: string; orderId: string; businessId: string; username: string; rating: number; comment?: string; createdAt: string; }

// FS-007 Business Dashboard — values originate from backend, never demo fixtures.
export type BusinessDashboard = {
  days: number;
  revenue: number;
  paidOrders: number;
  completedOrders: number;
  averageCheck: number;
  foodSavedKg: number;
  eligiblePaidFoodKg: number;
  rescuedFoodPercent: number;
  estimatedCo2SavedKg: number;
  revenueByDay: Array<{date: string; revenue: number; payments: number}>;
  topProducts: Array<{listingId: string; title: string; quantitySold: number; completedOrders: number; revenue: number}>;
};

export type AiListingDraft = {
  detectedProduct: string;
  title: string;
  description: string;
  suggestedCategory: FoodCategory;
  ocrText: string | null;
  label: {brand:string|null;netWeight:string|null;ingredients:string|null;allergens:string|null;expiryDateText:string|null;storageConditions:string|null;calories:number|null;proteins:number|null;fats:number|null;carbs:number|null};
  warnings: string[];
};

export type ListingImage = {id:string;listingId:string;contentUrl:string;sortOrder:number};

// Admin panel — values come from /api/admin/*, never from fixtures.
export interface AdminStats {
  totalUsers: number; blockedUsers: number; pendingApplications: number; pendingListings: number; usersByRole: Record<Role, number>;
  totalListings: number; listingsByStatus: Record<string, number>;
  totalOrders: number; ordersByStatus: Record<string, number>;
  revenue: number; paidOrders: number; foodSavedKg: number; co2SavedKg: number;
  activity: Array<{date: string; orders: number; revenue: number}>;
  topBusinesses: Array<{businessName: string; city: City | null; completedOrders: number; revenue: number}>;
}
export interface AdminUser {
  id: string; username: string; email: string; phoneNumber: string; city: City; role: Role;
  emailVerified: boolean; blocked: boolean; blockedReason: string | null;
  foodPoints: number; totalOrders: number; completedOrders: number; noShowCount: number;
  businessName: string | null;
}
export interface AdminListing {
  id: string; title: string; businessName: string; ownerEmail: string; city: City | null; status: ListingStatus;
  originalPrice: number; foodSavePrice: number; quantity: number;
  pickupStart: string; pickupEnd: string; expiresAt: string;
  moderationStatus: ModerationStatus; moderationReason: string | null;
}
export interface AdminApplication {
  id: string; businessName: string; businessType: BusinessType | null; description: string | null;
  address: string; latitude: number | null; longitude: number | null; city: City | null;
  contactPhone: string | null; bin: string | null; ownerUsername: string; ownerEmail: string;
  status: ModerationStatus; rejectionReason: string | null;
  aiVerdict: "APPROVE" | "REVIEW" | "REJECT" | "UNAVAILABLE" | null; aiNotes: string[];
  submittedAt: string | null; reviewedAt: string | null;
}
export interface AdminOrder {
  id: string; customerUsername: string; customerEmail: string; listingTitle: string; businessName: string;
  quantity: number; totalPrice: number; status: OrderStatus; paymentStatus: PaymentStatus | null;
  createdAt: string; pickupEnd: string;
}
