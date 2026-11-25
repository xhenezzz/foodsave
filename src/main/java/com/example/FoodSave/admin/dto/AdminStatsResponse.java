package com.example.FoodSave.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Platform-wide counters for the admin dashboard. All values come from the database. */
public record AdminStatsResponse(
        long totalUsers,
        long blockedUsers,
        long pendingApplications,
        long pendingListings,
        Map<String, Long> usersByRole,
        long totalListings,
        Map<String, Long> listingsByStatus,
        long totalOrders,
        Map<String, Long> ordersByStatus,
        BigDecimal revenue,
        long paidOrders,
        BigDecimal foodSavedKg,
        BigDecimal co2SavedKg,
        List<DayPoint> activity,
        List<TopBusiness> topBusinesses
) {
    public record DayPoint(LocalDate date, long orders, BigDecimal revenue) {}

    public record TopBusiness(String businessName, String city, long completedOrders, BigDecimal revenue) {}
}
