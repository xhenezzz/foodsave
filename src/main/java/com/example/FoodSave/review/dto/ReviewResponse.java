package com.example.FoodSave.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;

    private UUID orderId;

    private UUID businessId;

    private String username;

    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;
}