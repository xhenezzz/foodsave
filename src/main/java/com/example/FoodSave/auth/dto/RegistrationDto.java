package com.example.FoodSave.auth.dto;

import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDto {

    @NotBlank(message = "Username обязателен")
    private String username;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(
            regexp = "^(\\+7|8)\\d{10}$",
            message = "Введите корректный номер телефона Казахстана"
    )
    private String phoneNumber;

    @NotBlank(message = "Пароль обязателен")
    @Size(
            min = 8,
            max = 64,
            message = "Пароль должен содержать от 8 до 64 символов"
    )
    private String password;
    @NotNull(message = "Город обязателен")
    private City city;
    @NotNull(message = "Роль обязательна")
    private Role role;
}