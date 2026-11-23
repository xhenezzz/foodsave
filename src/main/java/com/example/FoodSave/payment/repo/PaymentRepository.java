package com.example.FoodSave.payment.repo;

import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    Optional<Payment> findByStripeCheckoutSessionId(String sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.stripeCheckoutSessionId = :sessionId")
    Optional<Payment> findByStripeCheckoutSessionIdForUpdate(@Param("sessionId") String sessionId);

    List<Payment> findByStatusAndStripeCheckoutSessionIdIsNotNull(PaymentStatus status);
}
