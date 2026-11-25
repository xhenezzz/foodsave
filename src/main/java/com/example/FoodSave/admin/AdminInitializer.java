package com.example.FoodSave.admin;

import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.entity.VerificationStatus;
import com.example.FoodSave.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Public registration never creates ADMIN accounts. The first administrator is created
 * at startup from ADMIN_EMAIL / ADMIN_PASSWORD (and optional ADMIN_USERNAME / ADMIN_PHONE).
 * An existing account with that email is never promoted or modified.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${foodsave.admin.email:}")
    private String email;
    @Value("${foodsave.admin.password:}")
    private String password;
    @Value("${foodsave.admin.username:admin}")
    private String username;
    @Value("${foodsave.admin.phone:+77000000000}")
    private String phone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        String normalizedEmail = email.trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            userRepository.findByEmail(normalizedEmail)
                    .filter(user -> user.getRole() != Role.ADMIN)
                    .ifPresent(user -> log.warn("ADMIN_EMAIL {} belongs to a non-admin account; it was not changed",
                            normalizedEmail));
            return;
        }
        if (password.length() < 8 || password.length() > 64) {
            log.warn("ADMIN_PASSWORD must contain 8-64 characters; administrator was not created");
            return;
        }
        if (!phone.matches("^(\\+7|8)\\d{10}$") || userRepository.existsByPhoneNumber(phone)) {
            log.warn("ADMIN_PHONE is invalid or already used; administrator was not created");
            return;
        }

        User admin = new User();
        admin.setUsername(username == null || username.isBlank() ? "admin" : username.trim());
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setPhoneNumber(phone);
        admin.setCity(City.ALMATY);
        admin.setRole(Role.ADMIN);
        admin.setVerificationStatus(VerificationStatus.VERIFIED);
        admin.setEmailVerified(true);
        admin.setFsId(UUID.randomUUID());
        admin.setFoodPoints(0);
        admin.setTotalOrders(0);
        admin.setCompletedOrders(0);
        admin.setNoShowCount(0);
        userRepository.save(admin);
        log.info("Administrator account {} created", normalizedEmail);
    }
}
