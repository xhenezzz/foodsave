package com.example.FoodSave.business.dto;

import com.example.FoodSave.auth.entity.City;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BusinessProfileResponse {

    private UUID id;

    private UUID userId;

    private String businessName;

    private String description;

    private String address;

    private Double latitude;

    private Double longitude;
}