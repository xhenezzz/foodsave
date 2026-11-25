package com.example.FoodSave.business.dto;

import com.example.FoodSave.business.entity.BusinessType;
import jakarta.validation.constraints.*;
import lombok.Data;

/** Business application form; the same fields are used to create and to edit a profile. */
@Data
public class BusinessProfileRequest {

    @NotBlank(message = "Укажи название бизнеса")
    @Size(max = 100, message = "Название бизнеса — до 100 символов")
    private String businessName;

    @NotNull(message = "Выбери тип заведения")
    private BusinessType businessType;

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

    @NotBlank(message = "Укажи контактный телефон")
    @Pattern(regexp = "^(\\+7|8)\\d{10}$", message = "Введите корректный номер телефона Казахстана")
    private String contactPhone;

    /** БИН/ИИН — placeholder for now: optional and not verified. */
    @Size(max = 12, message = "БИН/ИИН — до 12 символов")
    private String bin;
}
