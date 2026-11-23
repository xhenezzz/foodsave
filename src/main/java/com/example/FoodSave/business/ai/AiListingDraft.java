package com.example.FoodSave.business.ai;

import com.example.FoodSave.foodlisting.entity.FoodCategory;
import java.util.List;

/** AI output is a draft only. It never creates or publishes a listing. */
public record AiListingDraft(
        String detectedProduct,
        String title,
        String description,
        FoodCategory suggestedCategory,
        String ocrText,
        LabelData label,
        List<String> warnings
) {
    public record LabelData(
            String brand,
            String netWeight,
            String ingredients,
            String allergens,
            String expiryDateText,
            String storageConditions
    ) {}
}
