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

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.ok(
                orderService.createOrder(request)
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(
                orderService.getMyOrders()
        );
    }

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

    @GetMapping("/{id}/qr")
    public ResponseEntity<OrderQrResponse> getOrderQr(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                orderService.getOrderQr(id)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                orderService.getOrderById(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id
    ) {
        orderService.cancelOrder(id);

        return ResponseEntity.noContent().build();
    }
}