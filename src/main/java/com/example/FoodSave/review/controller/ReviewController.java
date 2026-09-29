package com.example.FoodSave.review.controller;

import com.example.FoodSave.review.dto.CreateReviewRequest;
import com.example.FoodSave.review.dto.ReviewResponse;
import com.example.FoodSave.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // CUSTOMER — оставить отзыв
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(
            @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.createReview(request)
        );
    }

    // BUSINESS — посмотреть отзывы своего бизнеса
    @GetMapping("/my")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        return ResponseEntity.ok(
                reviewService.getMyReviews()
        );
    }

    // BUSINESS — посмотреть рейтинг своего бизнеса
    @GetMapping("/my/rating")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<Double> getMyRating() {
        return ResponseEntity.ok(
                reviewService.getMyRating()
        );
    }
}