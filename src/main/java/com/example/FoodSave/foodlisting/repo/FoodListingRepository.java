package com.example.FoodSave.foodlisting.repo;

import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FoodListingRepository
        extends JpaRepository<FoodListing, UUID> {

    List<FoodListing> findByStatus(ListingStatus status);

    List<FoodListing> findByBusinessId(UUID businessId);

    @Query(value = """
            SELECT fl.*
            FROM food_listings fl
            JOIN business_profiles bp
                ON bp.id = fl.business_id
            WHERE fl.status = 'ACTIVE'
              AND bp.latitude IS NOT NULL
              AND bp.longitude IS NOT NULL
              AND (
                    6371 * acos(
                        cos(radians(:latitude))
                        * cos(radians(bp.latitude))
                        * cos(radians(bp.longitude) - radians(:longitude))
                        + sin(radians(:latitude))
                        * sin(radians(bp.latitude))
                    )
                  ) <= :radius
            ORDER BY (
                    6371 * acos(
                        cos(radians(:latitude))
                        * cos(radians(bp.latitude))
                        * cos(radians(bp.longitude) - radians(:longitude))
                        + sin(radians(:latitude))
                        * sin(radians(bp.latitude))
                    )
            )
            """,
            nativeQuery = true)
    List<FoodListing> findNearbyListings(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius
    );
}