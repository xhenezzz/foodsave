package com.example.FoodSave.admin;

import com.example.FoodSave.admin.dto.*;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.moderation.ModerationStatus;
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
                                               @RequestParam(required = false) ListingStatus status,
                                               @RequestParam(required = false) ModerationStatus moderation) {
        return adminService.listings(query, status, moderation);
    }

    @PostMapping("/listings/{id}/approve")
    public AdminListingResponse approveListing(@PathVariable UUID id) {
        return adminService.approveListing(id);
    }

    @PostMapping("/listings/{id}/reject")
    public AdminListingResponse rejectListing(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return adminService.rejectListing(id, request.reason());
    }

    @GetMapping("/applications")
    public List<AdminApplicationResponse> applications(@RequestParam(required = false) ModerationStatus status) {
        return adminService.applications(status);
    }

    @PostMapping("/applications/{id}/approve")
    public AdminApplicationResponse approveApplication(@PathVariable UUID id) {
        return adminService.approveApplication(id);
    }

    @PostMapping("/applications/{id}/reject")
    public AdminApplicationResponse rejectApplication(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return adminService.rejectApplication(id, request.reason());
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
