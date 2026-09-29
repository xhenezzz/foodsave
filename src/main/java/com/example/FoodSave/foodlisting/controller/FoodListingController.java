package com.example.FoodSave.foodlisting.controller;

import com.example.FoodSave.foodlisting.dto.CreateFoodListingRequest;
import com.example.FoodSave.foodlisting.dto.FoodListingResponse;
import com.example.FoodSave.foodlisting.dto.UpdateFoodListingRequest;
import com.example.FoodSave.foodlisting.service.FoodListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class FoodListingController {

    private final FoodListingService foodListingService;

    // BUSINESS — создать объявление
    @PostMapping
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<FoodListingResponse> createListing(
            @RequestBody CreateFoodListingRequest request
    ) {
        return ResponseEntity.ok(
                foodListingService.createListing(request)
        );
    }

    // CUSTOMER — посмотреть активные объявления
    @GetMapping
    public ResponseEntity<List<FoodListingResponse>> getActiveListings() {
        return ResponseEntity.ok(
                foodListingService.getActiveListings()
        );
    }

    // Любой авторизованный пользователь — посмотреть объявление
    @GetMapping("/{id}")
    public ResponseEntity<FoodListingResponse> getListingById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                foodListingService.getListingById(id)
        );
    }

    // BUSINESS — свои объявления
    @GetMapping("/my")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<FoodListingResponse>> getMyListings() {
        return ResponseEntity.ok(
                foodListingService.getMyListings()
        );
    }

    // BUSINESS — изменить своё объявление
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<FoodListingResponse> updateListing(
            @PathVariable UUID id,
            @RequestBody UpdateFoodListingRequest request
    ) {
        return ResponseEntity.ok(
                foodListingService.updateListing(id, request)
        );
    }

    // BUSINESS — отменить своё объявление
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<Void> cancelListing(
            @PathVariable UUID id
    ) {
        foodListingService.cancelListing(id);

        return ResponseEntity.noContent().build();
    }

    // CUSTOMER — объявления рядом
    @GetMapping("/nearby")
    public ResponseEntity<List<FoodListingResponse>> getNearbyListings(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") double radius
    ) {
        return ResponseEntity.ok(
                foodListingService.getNearbyListings(
                        latitude,
                        longitude,
                        radius
                )
        );
    }
}