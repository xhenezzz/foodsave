package com.example.FoodSave.business.entity;

import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.moderation.ModerationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A business profile doubles as the business's application: it can publish listings
 * only after an administrator sets {@link #status} to APPROVED.
 */
@Entity
@Table(name = "business_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String businessName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String address;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 32)
    private BusinessType businessType;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /** БИН/ИИН. Stored as entered; not verified against any registry yet. */
    @Column(length = 12)
    private String bin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModerationStatus status = ModerationStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Advisory AI recommendation: APPROVE, REVIEW, REJECT or UNAVAILABLE. */
    @Column(name = "ai_verdict", length = 16)
    private String aiVerdict;

    /** AI remarks, one per line. */
    @Column(name = "ai_notes", length = 2000)
    private String aiNotes;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
