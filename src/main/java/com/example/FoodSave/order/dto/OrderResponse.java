package com.example.FoodSave.order.dto;

import com.example.FoodSave.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OrderResponse {

    private UUID id;

    private UUID fsId;

    private UUID listingId;

    private String listingTitle;

    private Integer quantity;

    private BigDecimal totalPrice;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private LocalDateTime pickupStart;

    private LocalDateTime pickupEnd;
}