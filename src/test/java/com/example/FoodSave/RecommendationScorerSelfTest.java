package com.example.FoodSave.foodlisting.service;

public final class RecommendationScorerSelfTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        double discountHigh = RecommendationScorer.calculate(2000, 500, 5, 4.5, null);
        double discountLow = RecommendationScorer.calculate(2000, 1500, 5, 4.5, null);
        require(discountHigh > discountLow, "high discount should rank higher");

        double near = RecommendationScorer.calculate(2000, 1000, 5, 4.5, 0.5);
        double far = RecommendationScorer.calculate(2000, 1000, 5, 4.5, 8.0);
        require(near > far, "near listing should rank higher");

        double unknownReviews = RecommendationScorer.calculate(2000, 1000, 5, 0.0, null);
        double nullReviews = RecommendationScorer.calculate(2000, 1000, 5, null, null);
        require(unknownReviews == nullReviews, "0 rating is unknown, not negative review");

        require(RecommendationScorer.calculate(0, -1, null, null, null) == 0,
                "invalid price score should be zero");
        require(RecommendationScorer.calculate(2000, 1000, 10, 5.0, 0.0) <= 100,
                "score cannot exceed 100");
        require(RecommendationScorer.calculate(2000, 1000, 10, 5.0, 0.0) >= 0,
                "score cannot be negative");
        System.out.println("OK: 6 recommendation checks passed");
    }
}
