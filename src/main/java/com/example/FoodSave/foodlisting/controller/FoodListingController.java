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

    @PostMapping
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<FoodListingResponse> createListing(
            @RequestBody CreateFoodListingRequest request) {
        return ResponseEntity.ok(foodListingService.createListing(request));
    }

    // /api/listings?city=ALMATY or ?city=ASTANA
    @GetMapping
    public ResponseEntity<List<FoodListingResponse>> getActiveListings(
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(foodListingService.getActiveListings(city));
    }

    // Fixed route for frontend's /listings/my
    @GetMapping("/my")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<FoodListingResponse>> getMyListings() {
        return ResponseEntity.ok(foodListingService.getMyListings());
    }

    // /api/listings/nearby?latitude=43.2389&longitude=76.8897&radius=10&city=ALMATY
    @GetMapping("/nearby")
    public ResponseEntity<List<FoodListingResponse>> getNearbyListings(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") double radius,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(foodListingService.getNearbyListings(latitude, longitude, radius, city));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodListingResponse> getListingById(@PathVariable UUID id) {
        return ResponseEntity.ok(foodListingService.getListingById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<FoodListingResponse> updateListing(
            @PathVariable UUID id,
            @RequestBody UpdateFoodListingRequest request) {
        return ResponseEntity.ok(foodListingService.updateListing(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<Void> cancelListing(@PathVariable UUID id) {
        foodListingService.cancelListing(id);
        return ResponseEntity.noContent().build();
    }
}
