package com.example.FoodSave.impact.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ImpactResponse {

    private BigDecimal foodSavedKg;

    private BigDecimal moneySaved;

    private BigDecimal co2SavedKg;
}