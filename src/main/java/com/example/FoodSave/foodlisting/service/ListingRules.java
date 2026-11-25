package com.example.FoodSave.foodlisting.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Food safety and price rules. Never rely only on the frontend. */
public final class ListingRules {
    private ListingRules() {}

    // OrderService allows a 15-minute pickup grace period.
    public static final int PICKUP_GRACE_MINUTES = 15;

    public static void validate(
            String title, String description,
            BigDecimal originalPrice, BigDecimal foodSavePrice,
            Integer quantity, BigDecimal weightKg,
            LocalDateTime pickupStart, LocalDateTime pickupEnd,
            LocalDateTime expiresAt, LocalDateTime productExpiresAt,
            LocalDateTime now, boolean allowUnchangedPastPickupStart) {

        if (title == null || title.isBlank() || title.trim().length() > 120)
            invalid("Название должно содержать от 1 до 120 символов");
        if (description != null && description.length() > 1000)
            invalid("Описание не должно превышать 1000 символов");
        if (originalPrice == null || originalPrice.signum() <= 0
                || foodSavePrice == null || foodSavePrice.signum() <= 0
                || originalPrice.compareTo(foodSavePrice) <= 0
                || originalPrice.scale() > 2 || foodSavePrice.scale() > 2)
            invalid("Цены должны быть положительными, с точностью до копеек, и со скидкой");
        if (quantity == null || quantity < 0 || quantity > 1_000_000)
            invalid("Количество должно быть от 0 до 1 000 000");
        if (weightKg == null || weightKg.signum() <= 0 || weightKg.scale() > 3)
            invalid("Вес должен быть больше 0, максимум три знака после запятой");
        if (pickupStart == null || pickupEnd == null || expiresAt == null || productExpiresAt == null)
            invalid("Укажи все даты, включая реальный срок годности продукта");
        if (!allowUnchangedPastPickupStart && !pickupStart.isAfter(now))
            invalid("Начало выдачи должно быть в будущем");
        if (!pickupEnd.isAfter(now) || !pickupEnd.isAfter(pickupStart))
            invalid("Окончание выдачи должно быть позже начала и позже текущего времени");
        if (expiresAt.isBefore(pickupEnd) || !expiresAt.isAfter(now))
            invalid("Срок объявления не может завершаться раньше окончания выдачи");
        if (!productExpiresAt.isAfter(pickupEnd.plusMinutes(PICKUP_GRACE_MINUTES)))
            invalid("Продукт должен оставаться годным на время выдачи и 15 минут после неё");
        if (expiresAt.isAfter(productExpiresAt))
            invalid("Срок действия объявления не может превышать срок годности продукта");
    }

    static final BigDecimal MAX_CALORIES = new BigDecimal("900");   // pure fat is ~900 kcal / 100 g
    static final BigDecimal HUNDRED_GRAMS = new BigDecimal("100");

    /**
     * Optional nutrition facts per 100 g. Every value may be empty; filled values must be
     * non-negative and proteins + fats + carbs cannot exceed 100 g.
     * A mismatch between calories and 4·P + 9·F + 4·C is only a frontend warning.
     */
    public static void validateNutrition(BigDecimal calories, BigDecimal proteins, BigDecimal fats, BigDecimal carbs) {
        if (calories != null && (calories.signum() < 0 || calories.compareTo(MAX_CALORIES) > 0 || calories.scale() > 2))
            invalid("Калорийность — от 0 до 900 ккал на 100 г");
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal macro : new BigDecimal[]{proteins, fats, carbs}) {
            if (macro == null) continue;
            if (macro.signum() < 0 || macro.compareTo(HUNDRED_GRAMS) > 0 || macro.scale() > 2)
                invalid("Белки, жиры и углеводы — от 0 до 100 г на 100 г продукта");
            sum = sum.add(macro);
        }
        if (sum.compareTo(HUNDRED_GRAMS) > 0)
            invalid("Сумма белков, жиров и углеводов не может превышать 100 г на 100 г продукта");
    }

    public static boolean safeForPickup(LocalDateTime productExpiresAt, LocalDateTime now) {
        return productExpiresAt != null && productExpiresAt.isAfter(now);
    }

    private static void invalid(String reason) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
