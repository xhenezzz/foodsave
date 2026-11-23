package com.example.FoodSave.payment.dto;

import com.example.FoodSave.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record StripeCheckoutSessionResponse(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String clientSecret
) {
}
