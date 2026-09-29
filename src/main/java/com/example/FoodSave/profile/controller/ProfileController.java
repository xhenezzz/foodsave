package com.example.FoodSave.profile.controller;

import com.example.FoodSave.profile.dto.ChangePasswordRequest;
import com.example.FoodSave.profile.dto.FoodSaveQrResponse;
import com.example.FoodSave.profile.dto.ProfileResponse;
import com.example.FoodSave.profile.dto.UpdateProfileRequest;
import com.example.FoodSave.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/qr")
    public ResponseEntity<FoodSaveQrResponse> getFoodSaveQr() {
        return ResponseEntity.ok(profileService.getFoodSaveQr());
    }
}