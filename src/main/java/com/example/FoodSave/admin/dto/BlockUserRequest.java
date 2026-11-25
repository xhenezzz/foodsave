package com.example.FoodSave.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlockUserRequest(
        @NotNull(message = "Укажи, блокировать ли пользователя") Boolean blocked,
        @Size(max = 255, message = "Причина — до 255 символов") String reason
) {}
