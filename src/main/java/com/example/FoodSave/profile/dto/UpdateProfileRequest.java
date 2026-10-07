package com.example.FoodSave.profile.dto;

import com.example.FoodSave.auth.entity.City;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(
            regexp = "^(\\+7|8)\\d{10}$",
            message = "Введите корректный номер телефона Казахстана"
    )
    private String phoneNumber;
    @NotNull(message = "Город обязателен")
    private City city;
}
