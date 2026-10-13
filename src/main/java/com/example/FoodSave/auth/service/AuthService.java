package com.example.FoodSave.auth.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.dto.AuthResponse;
import com.example.FoodSave.auth.dto.LoginDto;
import com.example.FoodSave.auth.dto.RegistrationDto;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.entity.VerificationStatus;
import com.example.FoodSave.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegistrationDto dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email уже занят");
        }
        if (repository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new RuntimeException("Номер телефона уже занят");
        }

        Role role = dto.getRole();
        if (role == null) {
            throw new IllegalArgumentException("Выберите тип аккаунта");
        }
        if (role != Role.CUSTOMER && role != Role.BUSINESS) {
            throw new IllegalArgumentException("Недопустимая роль");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCity(dto.getCity());
        user.setFsId(UUID.randomUUID());

        // CRITICAL: your previous code validated role but never assigned it.
        user.setRole(role);

        user.setVerificationStatus(VerificationStatus.UNVERIFIED);
        user.setFoodPoints(0);
        user.setTotalOrders(0);
        user.setCompletedOrders(0);
        user.setNoShowCount(0);

        User savedUser = repository.save(user);
        return new AuthResponse(jwtService.generateToken(new UserPrincipal(savedUser)));
    }

    public AuthResponse login(LoginDto loginDto) {
        User user = repository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Неверный логин или пароль"));
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный логин или пароль");
        }
        String jwt = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(jwt);
    }
}
