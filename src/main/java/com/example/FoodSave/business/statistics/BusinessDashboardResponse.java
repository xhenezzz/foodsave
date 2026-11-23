package com.example.FoodSave.business.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BusinessDashboardResponse(
        int days,
        BigDecimal revenue,
        long paidOrders,
        long completedOrders,
        BigDecimal averageCheck,
        BigDecimal foodSavedKg,
        BigDecimal eligiblePaidFoodKg,
        BigDecimal rescuedFoodPercent,
        BigDecimal estimatedCo2SavedKg,
        List<DayRevenue> revenueByDay,
        List<TopProduct> topProducts
) {
    public record DayRevenue(LocalDate date, BigDecimal revenue, long payments) {}
    public record TopProduct(UUID listingId, String title, long quantitySold,
                             long completedOrders, BigDecimal revenue) {}
}
