package com.example.FoodSave.payment.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.payment.dto.DemoPaymentDecision;
import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import com.example.FoodSave.payment.repo.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentService service = new PaymentService(payments, orders);
    private final UUID orderId = UUID.randomUUID();
    private Order order;

    @BeforeEach
    void setup() {
        reset(payments, orders);
        ReflectionTestUtils.setField(service, "demoEnabled", true);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.CUSTOMER);
        var principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        FoodListing listing = new FoodListing();
        LocalDateTime future = LocalDateTime.now(ZoneId.of("Asia/Almaty")).plusHours(3);
        listing.setExpiresAt(future);
        listing.setProductExpiresAt(future.plusHours(3));
        order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setListing(listing);
        order.setStatus(OrderStatus.RESERVED);
        order.setTotalPrice(new BigDecimal("2500.00"));
        order.setExpiresAt(future);
        order.setPickupEnd(future);
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}

    @Test void startDoesNotSetSuccess() {
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        var response = service.startDemoPayment(orderId);
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        verify(payments).save(argThat(p -> p.getStatus() == PaymentStatus.PENDING));
    }

    @Test void approveAfterPendingMarksSuccess() {
        Payment payment = pending();
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        assertEquals(PaymentStatus.SUCCESS,
                service.confirmDemoPayment(orderId, DemoPaymentDecision.APPROVE).getStatus());
    }

    @Test void declineAfterPendingMarksFailed() {
        Payment payment = pending();
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        assertEquals(PaymentStatus.FAILED,
                service.confirmDemoPayment(orderId, DemoPaymentDecision.DECLINE).getStatus());
    }

    @Test void cannotConfirmWithoutSession() {
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> service.confirmDemoPayment(orderId, DemoPaymentDecision.APPROVE));
    }

    @Test void repeatedSuccessIsIdempotent() {
        Payment payment = pending();
        payment.setStatus(PaymentStatus.SUCCESS);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        assertEquals(PaymentStatus.SUCCESS,
                service.confirmDemoPayment(orderId, DemoPaymentDecision.APPROVE).getStatus());
        verify(payments, never()).save(any(Payment.class));
    }

    @Test void cannotPaySomeoneElsesOrder() {
        User differentUser = new User();differentUser.setId(UUID.randomUUID());
        order.setUser(differentUser);
        assertThrows(ResponseStatusException.class, () -> service.startDemoPayment(orderId));
        verifyNoInteractions(payments);
    }

    @Test void refusesExpiredOrder() {
        order.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Almaty")).minusMinutes(1));
        assertThrows(ResponseStatusException.class, () -> service.startDemoPayment(orderId));
    }

    @Test void refusesWhenDemoDisabled() {
        ReflectionTestUtils.setField(service, "demoEnabled", false);
        assertThrows(ResponseStatusException.class, () -> service.startDemoPayment(orderId));
        verifyNoInteractions(orders, payments);
    }

    private Payment pending() {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());p.setOrder(order);
        p.setAmount(order.getTotalPrice());p.setStatus(PaymentStatus.PENDING);
        p.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Almaty")));
        return p;
    }
}
