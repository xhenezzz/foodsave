package com.example.FoodSave.admin.dto;

import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderResponse(
        UUID id,
        String customerUsername,
        String customerEmail,
        String listingTitle,
        String businessName,
        int quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        LocalDateTime pickupEnd
) {}
