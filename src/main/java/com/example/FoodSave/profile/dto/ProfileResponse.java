package com.example.FoodSave.profile.dto;

import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.VerificationStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class ProfileResponse {

    private UUID id;
    private UUID fsId;

    private String username;
    private String email;
    private String phoneNumber;
    private String city;

    private Role role;
    private VerificationStatus verificationStatus;

    private Integer foodPoints;
    private Integer completedOrders;
    private Integer noShowCount;

    private Double reliability;

    public ProfileResponse(
            UUID id,
            UUID fsId,
            String username,
            String email,
            String phoneNumber,
            String city,
            Role role,
            VerificationStatus verificationStatus,
            Integer foodPoints,
            Integer completedOrders,
            Integer noShowCount,
            Double reliability
    ) {
        this.id = id;
        this.fsId = fsId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.role = role;
        this.verificationStatus = verificationStatus;
        this.foodPoints = foodPoints;
        this.completedOrders = completedOrders;
        this.noShowCount = noShowCount;
        this.reliability = reliability;
    }
}