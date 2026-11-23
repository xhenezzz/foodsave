package com.example.FoodSave.payment.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import com.example.FoodSave.payment.repo.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
import static org.mockito.Mockito.*;

/** Stripe checks that run before any network call to Stripe. */
class PaymentServiceTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final StripePaymentService service =
            new StripePaymentService(payments, orders, new ObjectMapper());
    private final UUID orderId = UUID.randomUUID();
    private User customer;
    private Order order;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "secretKey", "sk_test_unit");
        ReflectionTestUtils.setField(service, "configuredCurrency", "kzt");
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");

        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setRole(Role.CUSTOMER);
        authenticate(customer);

        FoodListing listing = new FoodListing();
        LocalDateTime future = LocalDateTime.now(ZoneId.of("Asia/Almaty")).plusHours(3);
        listing.setExpiresAt(future);
        listing.setProductExpiresAt(future.plusHours(3));
        order = new Order();
        order.setId(orderId);
        order.setUser(customer);
        order.setListing(listing);
        order.setStatus(OrderStatus.RESERVED);
        order.setTotalPrice(new BigDecimal("2500.00"));
        order.setExpiresAt(future);
        order.setPickupEnd(future);
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutFailsClearlyWhenStripeIsNotConfigured() {
        ReflectionTestUtils.setField(service, "secretKey", "");
        var error = assertThrows(ResponseStatusException.class, () -> service.startCheckout(orderId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verifyNoInteractions(payments);
    }

    @Test
    void cannotPaySomeoneElsesOrder() {
        User other = new User();
        other.setId(UUID.randomUUID());
        order.setUser(other);
        var error = assertThrows(ResponseStatusException.class, () -> service.startCheckout(orderId));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(payments);
    }

    @Test
    void businessCannotPay() {
        User business = new User();
        business.setId(UUID.randomUUID());
        business.setRole(Role.BUSINESS);
        authenticate(business);
        var error = assertThrows(ResponseStatusException.class, () -> service.startCheckout(orderId));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void refusesExpiredOrder() {
        order.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Almaty")).minusMinutes(1));
        var error = assertThrows(ResponseStatusException.class, () -> service.startCheckout(orderId));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(payments);
    }

    @Test
    void refusesCancelledOrder() {
        order.setStatus(OrderStatus.CANCELLED);
        var error = assertThrows(ResponseStatusException.class, () -> service.startCheckout(orderId));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void alreadyPaidOrderIsReturnedWithoutNewSession() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        var response = service.startCheckout(orderId);
        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertNull(response.clientSecret());
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void paymentStatusIsReadFromServer() {
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment(PaymentStatus.PENDING)));
        assertEquals(PaymentStatus.PENDING, service.getPaymentStatus(orderId).getStatus());
    }

    @Test
    void paymentStatusIsNotFoundBeforeCheckout() {
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.getPaymentStatus(orderId));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private void authenticate(User user) {
        var principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Payment payment(PaymentStatus status) {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setOrder(order);
        p.setAmount(order.getTotalPrice());
        p.setCurrency("KZT");
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Almaty")));
        return p;
    }
}
