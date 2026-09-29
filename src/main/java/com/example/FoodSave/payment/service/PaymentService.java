package com.example.FoodSave.payment.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.payment.dto.PaymentResponse;
import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import com.example.FoodSave.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse payForOrder(UUID orderId) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Заказ не найден"
                                )
                        );

        // Проверяем владельца заказа
        if (!order.getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете оплатить чужой заказ"
            );
        }

        // Оплатить можно только RESERVED
        if (order.getStatus()
                != OrderStatus.RESERVED) {

            throw new RuntimeException(
                    "Этот заказ нельзя оплатить"
            );
        }

        // Проверяем, не был ли заказ уже оплачен
        if (paymentRepository
                .existsByOrderId(orderId)) {

            throw new RuntimeException(
                    "Заказ уже оплачен"
            );
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setAmount(
                order.getTotalPrice()
        );

        // Пока используем mock payment
        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        payment.setCreatedAt(
                LocalDateTime.now()
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        return toResponse(savedPayment);
    }

    private PaymentResponse toResponse(
            Payment payment
    ) {

        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}