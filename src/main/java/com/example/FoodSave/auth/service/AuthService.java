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

        if (validateByEmail(dto.getEmail())) {
            throw new RuntimeException(
                    "Email уже занят"
            );
        }

        if (validateByPhoneNumber(dto.getPhoneNumber())) {
            throw new RuntimeException(
                    "Номер телефона уже занят"
            );
        }

        if (dto.getRole() == Role.ADMIN) {
            throw new RuntimeException(
                    "Регистрация ADMIN запрещена"
            );
        }

        User user = new User();

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(
                passwordEncoder.encode(dto.getPassword())
        );
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCity(dto.getCity());

        user.setFsId(UUID.randomUUID());

        user.setRole(dto.getRole());

        user.setVerificationStatus(
                VerificationStatus.UNVERIFIED
        );

        user.setFoodPoints(0);
        user.setTotalOrders(0);
        user.setCompletedOrders(0);
        user.setNoShowCount(0);

        User savedUser = repository.save(user);

        return new AuthResponse(
                jwtService.generateToken(
                        new UserPrincipal(savedUser)
                )
        );
    }

    public AuthResponse login(LoginDto loginDto) {

        User user = repository.findByEmail(loginDto.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Неверный логин или пароль"
                        )
                );

        if (!passwordEncoder.matches(
                loginDto.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException(
                    "Неверный логин или пароль"
            );
        }

        String jwt =
                jwtService.generateToken(
                        new UserPrincipal(user)
                );

        return new AuthResponse(jwt);
    }

    private boolean validateByEmail(String email) {
        return repository.existsByEmail(email);
    }

    private boolean validateByPhoneNumber(String phoneNumber) {
        return repository.existsByPhoneNumber(phoneNumber);
    }
}