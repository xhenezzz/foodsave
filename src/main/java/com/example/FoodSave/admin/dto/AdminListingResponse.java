package com.example.FoodSave.admin.dto;

import com.example.FoodSave.foodlisting.entity.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminListingResponse(
        UUID id,
        String title,
        String businessName,
        String ownerEmail,
        String city,
        ListingStatus status,
        BigDecimal originalPrice,
        BigDecimal foodSavePrice,
        int quantity,
        LocalDateTime pickupStart,
        LocalDateTime pickupEnd,
        LocalDateTime expiresAt
) {}
