package com.example.FoodSave.payment.controller;

import com.example.FoodSave.payment.dto.PaymentResponse;
import com.example.FoodSave.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> payForOrder(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(
                paymentService.payForOrder(orderId)
        );
    }
}