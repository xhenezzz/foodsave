package com.example.FoodSave.foodlisting.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ListingRulesTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 4, 12, 0);
    private final LocalDateTime start = now.plusHours(1);
    private final LocalDateTime end = now.plusHours(3);
    private final LocalDateTime listingEnd = end.plusMinutes(15);
    private final LocalDateTime foodExpiry = end.plusMinutes(30);

    private void validate(BigDecimal price, BigDecimal discounted,
                          Integer quantity, LocalDateTime pickupStart,
                          LocalDateTime pickupEnd, LocalDateTime offerExpiry,
                          LocalDateTime actualExpiry, boolean pastStartAllowed) {
        ListingRules.validate("Круассан", "Тест", price, discounted,
                quantity, new BigDecimal("0.450"), pickupStart, pickupEnd,
                offerExpiry, actualExpiry, now, pastStartAllowed);
    }

    @Test void acceptsSafeProduct() {
        assertDoesNotThrow(() -> validate(new BigDecimal("2000"), new BigDecimal("1000"),
                3, start, end, listingEnd, foodExpiry, false));
    }
    @Test void rejectsMissingProductExpiry() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                new BigDecimal("1000"), 3, start, end, listingEnd, null, false));
    }
    @Test void rejectsExpiryBeforeEndOfGrace() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                new BigDecimal("1000"), 3, start, end, listingEnd, end.plusMinutes(10), false));
    }
    @Test void rejectsNegativeOriginalPrice() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("-2000"),
                new BigDecimal("1000"), 3, start, end, listingEnd, foodExpiry, false));
    }
    @Test void rejectsNonDiscountedPrice() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                new BigDecimal("2000"), 3, start, end, listingEnd, foodExpiry, false));
    }
    @Test void rejectsNullAndNegativeQuantity() {
        for (Integer n : new Integer[]{null, -1})
            assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                    new BigDecimal("1000"), n, start, end, listingEnd, foodExpiry, false));
    }
    @Test void rejectsInvalidTimeOrder() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                new BigDecimal("1000"), 3, start, start.minusMinutes(5), listingEnd, foodExpiry, false));
    }
    @Test void rejectsPastPickupStartOnCreate() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                new BigDecimal("1000"), 3, now.minusMinutes(15), end, listingEnd, foodExpiry, false));
    }
    @Test void allowsUnchangedPastStartForEditingLiveListing() {
        assertDoesNotThrow(() -> validate(new BigDecimal("2000"),
                new BigDecimal("1000"), 3, now.minusMinutes(15), end, listingEnd, foodExpiry, true));
    }
    @Test void rejectsFoodExpirationAtPickupEnd() {
        assertFalse(ListingRules.safeForPickup(null, now));
        assertFalse(ListingRules.safeForPickup(now, now));
        assertTrue(ListingRules.safeForPickup(now.plusSeconds(1), now));
    }
    @Test void rejectsOfferExpiryAfterFoodExpiry() {
        assertThrows(ResponseStatusException.class, () -> validate(new BigDecimal("2000"),
                new BigDecimal("1000"), 3, start, end, end.plusHours(2), foodExpiry, false));
    }
}
