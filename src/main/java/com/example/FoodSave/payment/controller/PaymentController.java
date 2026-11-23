package com.example.FoodSave.payment.controller;

import com.example.FoodSave.payment.dto.PaymentResponse;
import com.example.FoodSave.payment.service.StripePaymentService;
import com.example.FoodSave.payment.dto.StripeCheckoutSessionResponse;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final StripePaymentService stripePaymentService;

    @PostMapping("/{orderId}/stripe/checkout-session")
    public ResponseEntity<StripeCheckoutSessionResponse> createStripeCheckoutSession(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(stripePaymentService.startCheckout(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable UUID orderId) {
        return ResponseEntity.ok(stripePaymentService.getPaymentStatus(orderId));
    }

    @PostMapping(value = "/stripe/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        stripePaymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
