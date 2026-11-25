package com.example.FoodSave.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A rejection always tells the business what to fix. */
public record RejectRequest(
        @NotBlank(message = "Укажи причину отклонения")
        @Size(max = 500, message = "Причина — до 500 символов")
        String reason
) {}
