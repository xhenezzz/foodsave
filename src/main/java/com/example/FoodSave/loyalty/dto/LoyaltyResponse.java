package com.example.FoodSave.loyalty.dto;

import com.example.FoodSave.loyalty.ReputationLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoyaltyResponse(
        UUID fsId,
        int foodPoints,
        int totalOrders,
        int completedOrders,
        int noShowCount,
        Integer reliabilityPercent,
        ReputationLevel reputationLevel,
        boolean canCreateOrder,
        LocalDateTime bookingBlockedUntil,
        int noShowsUntilNextBlock
) {}
