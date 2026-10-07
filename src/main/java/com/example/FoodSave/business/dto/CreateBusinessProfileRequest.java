package com.example.FoodSave.business.dto;

import com.example.FoodSave.auth.entity.City;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBusinessProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String businessName;

    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String address;
}