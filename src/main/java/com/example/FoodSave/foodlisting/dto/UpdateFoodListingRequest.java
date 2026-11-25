package com.example.FoodSave.foodlisting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.FoodSave.foodlisting.entity.FoodCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFoodListingRequest {
    // Required optimistic token from the last GET /api/listings/my.
    private Long version;
    private String title;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal foodSavePrice;
    private Integer quantity;
    private LocalDateTime pickupStart;
    private LocalDateTime pickupEnd;
    private LocalDateTime expiresAt;

    private LocalDateTime productExpiresAt;
    private BigDecimal weightKg;
    private FoodCategory category;

    // Optional nutrition facts per 100 g.
    private BigDecimal calories;
    private BigDecimal proteins;
    private BigDecimal fats;
    private BigDecimal carbs;
}
