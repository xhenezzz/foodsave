package com.example.FoodSave.admin;

import com.example.FoodSave.admin.dto.*;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.order.entity.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Platform administration. Every endpoint requires the ADMIN role. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) String query,
                                         @RequestParam(required = false) Role role) {
        return adminService.users(query, role);
    }

    @PutMapping("/users/{id}/blocked")
    public AdminUserResponse setBlocked(@PathVariable UUID id, @Valid @RequestBody BlockUserRequest request) {
        return adminService.setBlocked(id, request.blocked(), request.reason());
    }

    @GetMapping("/listings")
    public List<AdminListingResponse> listings(@RequestParam(required = false) String query,
                                               @RequestParam(required = false) ListingStatus status) {
        return adminService.listings(query, status);
    }

    @PostMapping("/listings/{id}/cancel")
    public AdminListingResponse cancelListing(@PathVariable UUID id) {
        return adminService.cancelListing(id);
    }

    @GetMapping("/orders")
    public List<AdminOrderResponse> orders(@RequestParam(required = false) OrderStatus status) {
        return adminService.orders(status);
    }
}
