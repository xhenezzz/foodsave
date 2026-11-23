package com.example.FoodSave.auth.repo;

import com.example.FoodSave.auth.entity.EmailVerificationToken;
import com.example.FoodSave.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    List<EmailVerificationToken> findByUserAndUsedAtIsNull(User user);
}
