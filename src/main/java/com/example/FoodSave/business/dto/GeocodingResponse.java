package com.example.FoodSave.business.dto;

public record GeocodingResponse(
        String address,
        double latitude,
        double longitude
) {
}
