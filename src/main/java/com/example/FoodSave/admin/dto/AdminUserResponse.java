package com.example.FoodSave.admin.dto;

import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.Role;

import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String email,
        String phoneNumber,
        City city,
        Role role,
        boolean emailVerified,
        boolean blocked,
        String blockedReason,
        int foodPoints,
        int totalOrders,
        int completedOrders,
        int noShowCount,
        String businessName
) {}
