package com.example.FoodSave.business.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiListingRequest(
        @NotBlank @Size(max = 120) String productName,
        @Size(max = 600) String details,
        String language
) {}
