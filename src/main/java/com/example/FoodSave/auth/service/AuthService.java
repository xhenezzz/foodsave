package com.example.FoodSave.auth.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.dto.AuthResponse;
import com.example.FoodSave.auth.dto.LoginDto;
import com.example.FoodSave.auth.dto.RegistrationDto;
import com.example.FoodSave.auth.dto.RegistrationResponse;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.entity.VerificationStatus;
import com.example.FoodSave.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    // Transactional: if the verification email cannot be sent, the user row is rolled back,
    // otherwise the account got stuck as "Email уже занят" without any way to verify it.
    @Transactional
    public RegistrationResponse register(RegistrationDto dto) {
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

        user.setRole(role);

        user.setVerificationStatus(VerificationStatus.UNVERIFIED);
        user.setFoodPoints(0);
        user.setTotalOrders(0);
        user.setCompletedOrders(0);
        user.setNoShowCount(0);
        user.setEmailVerified(false);

        User savedUser = repository.save(user);
        String raw = emailVerificationService.createVerificationToken(savedUser);
        emailVerificationService.sendVerificationEmail(user.getEmail(), raw);
        return new RegistrationResponse(
                "Письмо для подтверждения отправлено на email"
        );
    }

    public AuthResponse login(LoginDto loginDto) {

        if (loginDto.getEmail() == null || loginDto.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }
        User user = repository.findByEmail(loginDto.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Подтвердите электронную почту"
            );
        }
        String jwt = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(jwt);
    }
}
