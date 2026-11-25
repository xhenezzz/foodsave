package com.example.FoodSave.foodlisting.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NutritionRulesTest {
    private static BigDecimal d(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static void validate(String kcal, String p, String f, String c) {
        ListingRules.validateNutrition(d(kcal), d(p), d(f), d(c));
    }

    @Test
    void nutritionIsOptional() {
        assertDoesNotThrow(() -> validate(null, null, null, null));
        assertDoesNotThrow(() -> validate("250", null, null, null));
    }

    @Test
    void acceptsRealisticLabel() {
        assertDoesNotThrow(() -> validate("406", "8.2", "21", "45.5"));
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(ResponseStatusException.class, () -> validate("-1", null, null, null));
        assertThrows(ResponseStatusException.class, () -> validate(null, "-0.5", null, null));
    }

    @Test
    void rejectsMacrosAboveHundredGrams() {
        assertThrows(ResponseStatusException.class, () -> validate(null, "40", "40", "30"));
        assertThrows(ResponseStatusException.class, () -> validate(null, null, "101", null));
    }

    @Test
    void rejectsImpossibleCaloriesAndTooPreciseValues() {
        assertThrows(ResponseStatusException.class, () -> validate("950", null, null, null));
        assertThrows(ResponseStatusException.class, () -> validate("250.123", null, null, null));
    }
}
