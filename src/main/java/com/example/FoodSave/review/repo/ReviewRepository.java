package com.example.FoodSave.review.repo;

import com.example.FoodSave.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderId(UUID orderId);

    List<Review> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    @Query("""
    SELECT AVG(r.rating)
    FROM Review r
    WHERE r.business.id = :businessId
""")
    Double getAverageRating(
            @Param("businessId") UUID businessId
    );
}