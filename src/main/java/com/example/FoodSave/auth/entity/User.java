package com.example.FoodSave.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String phoneNumber;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String city;
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
    @Column(unique = true, nullable = false)
    private UUID fsId;
    private Integer foodPoints = 0;
    private Integer totalOrders = 0;
    private Integer completedOrders = 0;
    private Integer noShowCount = 0;

}
