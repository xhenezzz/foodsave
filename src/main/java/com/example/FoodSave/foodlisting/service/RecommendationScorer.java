package com.example.FoodSave.foodlisting.service;

/**
 * FoodSave rule-based recommendation v1 — Java 17+.
 *
 * Copy into your backend and add your package declaration.
 * This class has no Spring or external library dependencies.
 * Results range from 0 to 100. Missing rating / position are not punished:
 * the weights of available factors are normalized automatically.
 */
public final class RecommendationScorer {
    private static final double DISCOUNT_WEIGHT = 0.40;
    private static final double RATING_WEIGHT = 0.25;
    private static final double QUANTITY_WEIGHT = 0.15;
    private static final double DISTANCE_WEIGHT = 0.20;

    private RecommendationScorer() { }

    public static double calculate(
            Number originalPrice, Number foodSavePrice, Integer quantity,
            Double businessRating, Double distanceKm) {
        double weighted = 0;
        double availableWeight = 0;

        if (originalPrice != null && foodSavePrice != null) {
            double original = originalPrice.doubleValue();
            double discounted = foodSavePrice.doubleValue();
            if (Double.isFinite(original) && Double.isFinite(discounted)
                    && original > 0 && discounted >= 0 && discounted <= original) {
                weighted += clamp01((original - discounted) / original) * DISCOUNT_WEIGHT;
                availableWeight += DISCOUNT_WEIGHT;
            }
        }
        // In the current FoodSave backend 0 may mean "no reviews".
        if (businessRating != null && Double.isFinite(businessRating)
                && businessRating > 0 && businessRating <= 5) {
            weighted += (businessRating / 5) * RATING_WEIGHT;
            availableWeight += RATING_WEIGHT;
        }
        if (quantity != null) {
            weighted += clamp01(quantity / 10.0) * QUANTITY_WEIGHT;
            availableWeight += QUANTITY_WEIGHT;
        }
        if (distanceKm != null && Double.isFinite(distanceKm) && distanceKm >= 0) {
            weighted += (1.0 / (1.0 + distanceKm)) * DISTANCE_WEIGHT;
            availableWeight += DISTANCE_WEIGHT;
        }
        if (availableWeight == 0) return 0;
        return Math.round(weighted / availableWeight * 1000) / 10.0;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
