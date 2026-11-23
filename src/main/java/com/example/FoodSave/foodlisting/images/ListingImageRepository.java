package com.example.FoodSave.foodlisting.images;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingImageRepository extends JpaRepository<ListingImage, UUID> {
    List<ListingImage> findByListingIdOrderBySortOrderAscCreatedAtAsc(UUID listingId);
    long countByListingId(UUID listingId);
    Optional<ListingImage> findByIdAndListingId(UUID imageId, UUID listingId);
}
