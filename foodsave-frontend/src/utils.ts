import type { City, Listing, Order } from "./types";

export const money = (value: number) => new Intl.NumberFormat("ru-RU").format(value) + " ₸";
export const dt = (value: string) => new Date(value).toLocaleString("ru-RU", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
export const time = (value: string) => new Date(value).toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });
export const disc = (original: number, foodSave: number) => original > 0 ? Math.max(0, Math.round((1 - foodSave / original) * 100)) : 0;
export const qrImg = (data: string) => `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(data)}`;
export const errText = (error: unknown, fallback: string) => error instanceof Error ? error.message : fallback;
export const cityName = (city: City) => city === "ALMATY" ? "Алматы" : "Астана";
export function toLocal(date: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// Только подтверждённый город из API или корректные координаты. Не угадываем по названию бизнеса.
const centers: Record<City, [number, number]> = {
  ALMATY: [43.238949, 76.889709],
  ASTANA: [51.169392, 71.449074]
};
export function kilometers(lat1: number, lon1: number, lat2: number, lon2: number) {
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(a));
}
/** Approximate city determination from coordinates as a temporary compatibility fallback.
 * The backend should return explicit city and enforce it in queries.
 */
export function coordinatesCity(latitude: number, longitude: number): City | null {
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)
      || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) return null;
  const [almatyLat, almatyLon] = centers.ALMATY;
  const [astanaLat, astanaLon] = centers.ASTANA;
  const almatyDistance = kilometers(latitude, longitude, almatyLat, almatyLon);
  const astanaDistance = kilometers(latitude, longitude, astanaLat, astanaLon);
  if (almatyDistance <= 90 && almatyDistance < astanaDistance) return "ALMATY";
  if (astanaDistance <= 90 && astanaDistance < almatyDistance) return "ASTANA";
  return null;
}

export function listingCity(listing: Listing): City | null {
  const explicitCity = listing.city === "ALMATY" || listing.city === "ASTANA" ? listing.city : null;
  const businessCity = listing.businessCity === "ALMATY" || listing.businessCity === "ASTANA" ? listing.businessCity : null;
  if (explicitCity && businessCity && explicitCity !== businessCity) return null;
  const known = explicitCity ?? businessCity;
  const hasCoordinates = typeof listing.latitude === "number" && typeof listing.longitude === "number"
    && Number.isFinite(listing.latitude) && Number.isFinite(listing.longitude);
  const fromCoords = hasCoordinates ? coordinatesCity(listing.latitude!, listing.longitude!) : null;
  // В случае явного противоречия полей API не показываем объявление.
  if (known && hasCoordinates && known !== fromCoords) return null;
  return known ?? fromCoords;
}

/** Frontend safety filter. This is NOT a substitute for server-side city filtering. */
export function listingsForCity(listings: Listing[], city: City): Listing[] {
  return listings.filter(listing => listingAvailable(listing) && listingCity(listing) === city);
}
export const listingAvailable = (listing: Listing) => listing.status === "ACTIVE" && listing.quantity > 0 && new Date(listing.expiresAt).getTime() > Date.now();
export const confirmedPayment = (status?: string | null) => ["SUCCESS", "SUCCEEDED", "PAID", "COMPLETED", "CAPTURED"].includes(String(status ?? "").toUpperCase());
export const orderPaidByServer = (order: Order) => order.paid === true || confirmedPayment(order.paymentStatus);
