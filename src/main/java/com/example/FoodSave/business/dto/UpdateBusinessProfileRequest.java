package com.example.FoodSave.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBusinessProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String businessName;

    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String address;
}