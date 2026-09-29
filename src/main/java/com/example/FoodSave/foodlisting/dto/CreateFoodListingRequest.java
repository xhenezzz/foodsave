package com.example.FoodSave.foodlisting.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFoodListingRequest {

    private String title;

    private String description;

    private BigDecimal originalPrice;

    private BigDecimal foodSavePrice;

    private Integer quantity;

    private LocalDateTime pickupStart;

    private LocalDateTime pickupEnd;

    private LocalDateTime expiresAt;

    private BigDecimal weightKg;
}