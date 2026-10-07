package com.example.FoodSave.auth.controller;

import com.example.FoodSave.auth.dto.AuthResponse;
import com.example.FoodSave.auth.dto.LoginDto;
import com.example.FoodSave.auth.dto.RegistrationDto;
import com.example.FoodSave.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegistrationDto dto
    ) {
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginDto dto
    ) {
        return ResponseEntity.ok(authService.login(dto));
    }
}