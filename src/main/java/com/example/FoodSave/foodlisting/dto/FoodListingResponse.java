package com.example.FoodSave.foodlisting.dto;

import com.example.FoodSave.foodlisting.entity.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FoodListingResponse {

    private UUID id;

    private String title;
    private String description;

    private BigDecimal originalPrice;
    private BigDecimal foodSavePrice;

    private Integer quantity;

    private LocalDateTime pickupStart;
    private LocalDateTime pickupEnd;
    private LocalDateTime expiresAt;

    private ListingStatus status;

    private String businessName;
    private Double businessRating;

    private Double distanceKm;
}