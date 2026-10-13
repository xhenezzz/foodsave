import type { Listing } from "./types";
import { disc, kilometers } from "./utils.ts";

export type Coordinates = { lat: number; lon: number };
export type ListingSort = "recommended" | "discount" | "price" | "distance";

// Transparent, rule-based v1. Weights are relative: missing rating/location
// are excluded and the remaining weights are renormalized.
const WEIGHTS = { discount: 0.4, rating: 0.25, quantity: 0.15, distance: 0.2 } as const;
const clamp01 = (value: number) => Math.min(1, Math.max(0, value));
const valid = (value: unknown): value is number => typeof value === "number" && Number.isFinite(value);

export function listingDistance(listing: Listing, user: Coordinates | null): number | null {
  if (!user || !valid(user.lat) || !valid(user.lon)) return null;
  // Prefer the user's current coordinates to a server-side stored distance.
  if (valid(listing.latitude) && valid(listing.longitude)
      && Math.abs(listing.latitude) <= 90 && Math.abs(listing.longitude) <= 180) {
    return kilometers(user.lat, user.lon, listing.latitude, listing.longitude);
  }
  return valid(listing.distanceKm) && listing.distanceKm >= 0 ? listing.distanceKm : null;
}

/** Fallback for older backend responses lacking recommendationScore. */
export function recommendationScore(listing: Listing, user: Coordinates | null = null): number {
  let sum = 0;
  let weight = 0;
  const add = (value: number | null, factor: number) => {
    if (value === null) return;
    sum += clamp01(value) * factor;
    weight += factor;
  };
  const discount = valid(listing.originalPrice) && valid(listing.foodSavePrice)
    && listing.originalPrice > 0 && listing.foodSavePrice >= 0 && listing.foodSavePrice <= listing.originalPrice
    ? (listing.originalPrice - listing.foodSavePrice) / listing.originalPrice : null;
  const rating = valid(listing.businessRating) && listing.businessRating > 0 && listing.businessRating <= 5
    ? listing.businessRating / 5 : null;
  const qty = valid(listing.quantity) ? Math.min(listing.quantity / 10, 1) : null;
  const distance = listingDistance(listing, user);
  add(discount, WEIGHTS.discount);
  add(rating, WEIGHTS.rating);
  add(qty, WEIGHTS.quantity);
  add(distance === null ? null : 1 / (1 + distance), WEIGHTS.distance);
  return weight === 0 ? 0 : Math.round((sum / weight) * 1000) / 10;
}

/** Backend uses 0–100. Invalid, missing or out-of-range values trigger a local fallback. */
export function backendRecommendationScore(listing: Listing): number | null {
  const score = listing.recommendationScore;
  return valid(score) && score >= 0 && score <= 100 ? score : null;
}

export function effectiveRecommendationScore(listing: Listing, user: Coordinates | null = null): number {
  return backendRecommendationScore(listing) ?? recommendationScore(listing, user);
}

export function recommendationReason(listing: Listing, user: Coordinates | null = null): string {
  const distance = listingDistance(listing, user);
  const discount = disc(listing.originalPrice, listing.foodSavePrice);
  if (discount >= 50) return `Выгодная скидка −${discount}%`;
  if (distance !== null && distance <= 2) return `Рядом: ${distance.toFixed(1)} км`;
  if (valid(listing.businessRating) && listing.businessRating >= 4.5) {
    return `Высокий рейтинг ${listing.businessRating.toFixed(1)} ★`;
  }
  return "Подобрано по выгоде и наличию";
}

export function sortListings(listings: Listing[], by: ListingSort, user: Coordinates | null = null): Listing[] {
  const price = (listing: Listing) => valid(listing.foodSavePrice) ? listing.foodSavePrice : Infinity;
  const savings = (listing: Listing) => valid(listing.originalPrice) && valid(listing.foodSavePrice) && listing.originalPrice > 0
    ? clamp01((listing.originalPrice - listing.foodSavePrice) / listing.originalPrice) : 0;
  return [...listings].sort((a, b) => {
    let difference = 0;
    if (by === "recommended") difference = effectiveRecommendationScore(b, user) - effectiveRecommendationScore(a, user);
    if (by === "discount") difference = savings(b) - savings(a);
    if (by === "price") difference = price(a) - price(b);
    if (by === "distance") difference = (listingDistance(a, user) ?? Infinity) - (listingDistance(b, user) ?? Infinity);
    if (difference !== 0 && !Number.isNaN(difference)) return difference;
    // A deterministic fallback when scores are tied.
    return a.id.localeCompare(b.id);
  });
}
