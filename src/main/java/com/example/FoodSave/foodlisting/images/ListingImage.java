package com.example.FoodSave.foodlisting.images;

import com.example.FoodSave.foodlisting.entity.FoodListing;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_images")
@Getter @Setter
public class ListingImage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private FoodListing listing;
    @Column(name = "stored_filename", nullable = false, unique = true, length = 100)
    private String storedFilename;
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
