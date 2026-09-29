package com.example.FoodSave.business.controller;

import com.example.FoodSave.business.dto.BusinessProfileResponse;
import com.example.FoodSave.business.dto.CreateBusinessProfileRequest;
import com.example.FoodSave.business.dto.UpdateBusinessProfileRequest;
import com.example.FoodSave.business.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS')")
public class BusinessProfileController {

    private final BusinessProfileService businessProfileService;

    @PostMapping
    public ResponseEntity<BusinessProfileResponse> createProfile(
            @Valid @RequestBody CreateBusinessProfileRequest request
    ) {

        return ResponseEntity.ok(
                businessProfileService.createProfile(request)
        );
    }

    @GetMapping
    public ResponseEntity<BusinessProfileResponse> getMyProfile() {

        return ResponseEntity.ok(
                businessProfileService.getMyProfile()
        );
    }

    @PutMapping
    public ResponseEntity<BusinessProfileResponse> updateProfile(
            @Valid @RequestBody UpdateBusinessProfileRequest request
    ) {

        return ResponseEntity.ok(
                businessProfileService.updateProfile(request)
        );
    }
}