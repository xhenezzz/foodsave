package com.example.FoodSave.business.dto;

import com.example.FoodSave.business.entity.BusinessType;
import com.example.FoodSave.moderation.ModerationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** What the business itself sees. The AI recommendation is shown to administrators only. */
public record BusinessProfileResponse(
        UUID id,
        UUID userId,
        String businessName,
        String description,
        String address,
        Double latitude,
        Double longitude,
        BusinessType businessType,
        String contactPhone,
        String bin,
        ModerationStatus status,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {}
