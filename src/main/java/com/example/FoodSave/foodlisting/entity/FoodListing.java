package com.example.FoodSave.foodlisting.entity;

import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.moderation.ModerationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private FoodCategory category;

    @Column(nullable = false)
    private BigDecimal originalPrice;

    @Column(nullable = false)
    private BigDecimal foodSavePrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime pickupStart;

    @Column(nullable = false)
    private LocalDateTime pickupEnd;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // The actual food safety expiration time; NULL on legacy unverified rows.
    @Column(name = "product_expires_at")
    private LocalDateTime productExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessProfile business;

    @Column(nullable = false)
    private BigDecimal weightKg;

    /** Only APPROVED listings are shown to customers. */
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 16)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;

    // Optional nutrition facts per 100 g, confirmed by the seller.
    @Column(precision = 7, scale = 2)
    private BigDecimal calories;

    @Column(precision = 6, scale = 2)
    private BigDecimal proteins;

    @Column(precision = 6, scale = 2)
    private BigDecimal fats;

    @Column(precision = 6, scale = 2)
    private BigDecimal carbs;
}