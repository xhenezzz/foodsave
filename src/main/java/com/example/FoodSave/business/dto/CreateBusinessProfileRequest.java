package com.example.FoodSave.business.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBusinessProfileRequest {

    @NotBlank(message = "Укажи название бизнеса")
    @Size(max = 100, message = "Название бизнеса — до 100 символов")
    private String businessName;

    @Size(max = 1000, message = "Описание — до 1000 символов")
    private String description;

    @NotBlank(message = "Укажи адрес")
    @Size(max = 255, message = "Адрес — до 255 символов")
    private String address;

    /** Point chosen on the map. When absent, the address is geocoded. */
    @DecimalMin(value = "-90.0", message = "Некорректная широта")
    @DecimalMax(value = "90.0", message = "Некорректная широта")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Некорректная долгота")
    @DecimalMax(value = "180.0", message = "Некорректная долгота")
    private Double longitude;
}
