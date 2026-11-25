package com.example.FoodSave.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;
    @Column(nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    @Pattern(
            regexp = "^(\\+7|8)\\d{10}$",
            message = "Введите корректный номер телефона Казахстана"
    )
    @NotBlank(message = "Номер телефона обязателен")
    private String phoneNumber;
    @Column(unique = true, nullable = false)
    @Email(message = "Некорректный email")
    @NotBlank(message = "Email обязателен")
    private String email;
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, max = 64, message = "Пароль должен содержать от 8 до 64 символов")
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Город обязателен")
    private City city;
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
    @Column(nullable = false)
    private boolean emailVerified ;
    @Column(name = "booking_blocked_until")
    private LocalDateTime bookingBlockedUntil;
    @Column(unique = true, nullable = false)
    private UUID fsId;
    private Integer foodPoints = 0;
    private Integer totalOrders = 0;
    private Integer completedOrders = 0;
    private Integer noShowCount = 0;
    /** Set by an administrator; blocks login and every request with an existing JWT. */
    @Column(nullable = false)
    private boolean blocked;
    @Column(name = "blocked_reason")
    private String blockedReason;

}
