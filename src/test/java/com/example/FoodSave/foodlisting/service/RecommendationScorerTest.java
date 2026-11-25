package com.example.FoodSave.foodlisting.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationScorerTest {

    @Test
    void higherDiscountRanksHigher() {
        assertTrue(RecommendationScorer.calculate(2000, 500, 5, 4.5, null)
                > RecommendationScorer.calculate(2000, 1500, 5, 4.5, null));
    }

    @Test
    void nearerListingRanksHigher() {
        assertTrue(RecommendationScorer.calculate(2000, 1000, 5, 4.5, 0.5)
                > RecommendationScorer.calculate(2000, 1000, 5, 4.5, 8.0));
    }

    @Test
    void zeroRatingMeansUnknownNotNegative() {
        assertEquals(RecommendationScorer.calculate(2000, 1000, 5, null, null),
                RecommendationScorer.calculate(2000, 1000, 5, 0.0, null));
    }

    @Test
    void scoreStaysWithinZeroToHundred() {
        assertEquals(0, RecommendationScorer.calculate(0, -1, null, null, null));
        double best = RecommendationScorer.calculate(2000, 1000, 10, 5.0, 0.0);
        assertTrue(best >= 0 && best <= 100);
    }
}
