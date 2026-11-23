package com.example.FoodSave.auth.controller;

import com.example.FoodSave.auth.dto.*;
import com.example.FoodSave.auth.service.AuthService;
import com.example.FoodSave.auth.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
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


    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @RequestParam String token
    ) {
        AuthResponse jwtToken = emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok(jwtToken);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @RequestBody ResendVerificationRequest request
    ) {
        emailVerificationService.resendEmailCode(request.email());

        return ResponseEntity.ok(
                Map.of("message", "Если адрес зарегистрирован, письмо будет отправлено")
        );
    }
}