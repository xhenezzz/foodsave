package com.example.FoodSave.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class FoodSaveQrResponse {

    private UUID fsId;
    private String qrData;
}