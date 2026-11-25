package com.example.FoodSave.foodlisting.repo;

import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodListingRepository extends JpaRepository<FoodListing, UUID> {

    List<FoodListing> findByStatus(ListingStatus status);

    List<FoodListing> findByBusinessId(UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FoodListing f WHERE f.id = :id")
    Optional<FoodListing> findByIdForUpdate(@Param("id") UUID id);

    // Находим объявления внутри радиуса. Ранжирование производится сервисом,
    // а не SQL ORDER BY distance, потому что score учитывает несколько факторов.
    @Query(value = """
            SELECT fl.*
            FROM food_listings fl
            JOIN business_profiles bp ON bp.id = fl.business_id
            WHERE fl.status = 'ACTIVE'
              AND fl.moderation_status = 'APPROVED'
              AND fl.quantity > 0
              AND bp.latitude IS NOT NULL
              AND bp.longitude IS NOT NULL
              AND (
                    6371.0 * acos(
                        LEAST(1.0, GREATEST(-1.0,
                            cos(radians(:latitude)) * cos(radians(bp.latitude))
                            * cos(radians(bp.longitude) - radians(:longitude))
                            + sin(radians(:latitude)) * sin(radians(bp.latitude))
                        ))
                    )
                  ) <= :radius
            """, nativeQuery = true)
    List<FoodListing> findNearbyListings(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius
    );
}
