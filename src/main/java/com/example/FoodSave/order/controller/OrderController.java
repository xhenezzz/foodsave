package com.example.FoodSave.order.controller;

import com.example.FoodSave.order.dto.CreateOrderRequest;
import com.example.FoodSave.order.dto.OrderQrResponse;
import com.example.FoodSave.order.dto.OrderResponse;
import com.example.FoodSave.order.dto.ScanOrderQrRequest;
import com.example.FoodSave.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // CUSTOMER — создать заказ
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.ok(
                orderService.createOrder(request)
        );
    }

    // CUSTOMER — свои заказы
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(
                orderService.getMyOrders()
        );
    }

    // CUSTOMER — получить свой заказ
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                orderService.getOrderById(id)
        );
    }

    // CUSTOMER — отменить свой заказ
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id
    ) {
        orderService.cancelOrder(id);

        return ResponseEntity.noContent().build();
    }

    // CUSTOMER — получить QR заказа
    @GetMapping("/{id}/qr")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderQrResponse> getOrderQr(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                orderService.getOrderQr(id)
        );
    }

    // BUSINESS — отсканировать QR клиента
    @PostMapping("/scan")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<OrderResponse> scanOrderQr(
            @RequestBody ScanOrderQrRequest request
    ) {
        return ResponseEntity.ok(
                orderService.completeOrderByQr(
                        request.getQrToken()
                )
        );
    }
}