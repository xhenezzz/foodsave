package com.example.FoodSave.order.repository;

import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime now);

    List<Order> findByStatusAndPickupEndBefore(OrderStatus status, LocalDateTime time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.qrToken = :qrToken")
    Optional<Order> findByQrTokenForUpdate(@Param("qrToken") String qrToken);

    @Query("""
            SELECT o FROM Order o
            WHERE o.user.id = :userId
            AND o.status = com.example.FoodSave.order.entity.OrderStatus.COMPLETED
            """)
    List<Order> findCompletedOrdersByUserId(@Param("userId") UUID userId);
}
