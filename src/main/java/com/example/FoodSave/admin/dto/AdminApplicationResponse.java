package com.example.FoodSave.admin.dto;

import com.example.FoodSave.business.entity.BusinessType;
import com.example.FoodSave.moderation.ModerationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Business application as the administrator sees it, including the advisory AI check. */
public record AdminApplicationResponse(
        UUID id,
        String businessName,
        BusinessType businessType,
        String description,
        String address,
        Double latitude,
        Double longitude,
        String city,
        String contactPhone,
        String bin,
        String ownerUsername,
        String ownerEmail,
        ModerationStatus status,
        String rejectionReason,
        String aiVerdict,
        List<String> aiNotes,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {}
