package com.example.FoodSave.payment.dto;

import jakarta.validation.constraints.NotNull;

public record DemoPaymentConfirmRequest(@NotNull DemoPaymentDecision decision) {}
