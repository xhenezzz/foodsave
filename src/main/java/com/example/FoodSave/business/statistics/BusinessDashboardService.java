package com.example.FoodSave.business.statistics;

import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessDashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
    private static final BigDecimal CO2_PER_KG = new BigDecimal("2.5"); // estimate, not measured
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public BusinessDashboardResponse getDashboard(UUID ownerId, int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new IllegalArgumentException("days must be 7, 30 or 90");
        }
        LocalDate today = LocalDate.now(ZONE);
        LocalDate firstDay = today.minusDays(days - 1L);
        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // Data isolation is in the WHERE clause; frontend can never select another owner.
        List<Payment> successfulPayments = entityManager.createQuery("""
                SELECT p FROM Payment p
                JOIN FETCH p.order o
                JOIN FETCH o.listing l
                WHERE l.business.user.id = :ownerId
                  AND p.status = :status
                  AND p.createdAt >= :start AND p.createdAt < :end
                """, Payment.class)
                .setParameter("ownerId", ownerId)
                .setParameter("status", PaymentStatus.SUCCESS)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        // There is no completedAt in Order: period represents ORDER CREATION date.
        List<Order> completed = entityManager.createQuery("""
                SELECT o FROM Order o JOIN FETCH o.listing l
                WHERE l.business.user.id = :ownerId
                  AND o.status = :status
                  AND o.createdAt >= :start AND o.createdAt < :end
                """, Order.class)
                .setParameter("ownerId", ownerId)
                .setParameter("status", OrderStatus.COMPLETED)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<LocalDate, BigDecimal> revenuePerDay = new TreeMap<>();
        Map<LocalDate, Long> paymentsPerDay = new HashMap<>();
        for (int i = 0; i < days; i++) {
            revenuePerDay.put(firstDay.plusDays(i), BigDecimal.ZERO);
        }
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal eligiblePaidWeight = BigDecimal.ZERO;
        for (Payment p : successfulPayments) {
            BigDecimal amount = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            revenue = revenue.add(amount);
            LocalDate day = p.getCreatedAt().toLocalDate();
            revenuePerDay.merge(day, amount, BigDecimal::add);
            paymentsPerDay.merge(day, 1L, Long::sum);
            eligiblePaidWeight = eligiblePaidWeight.add(orderWeight(p.getOrder()));
        }

        BigDecimal saved = BigDecimal.ZERO;
        Map<UUID, ProductCounter> top = new HashMap<>();
        for (Order o : completed) {
            saved = saved.add(orderWeight(o));
            UUID id = o.getListing().getId();
            ProductCounter counter = top.computeIfAbsent(id,
                    ignored -> new ProductCounter(id, o.getListing().getTitle()));
            counter.quantity += o.getQuantity() == null ? 0L : o.getQuantity();
            counter.completedOrders++;
            counter.revenue = counter.revenue.add(
                    o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice());
        }

        List<BusinessDashboardResponse.DayRevenue> series = revenuePerDay.entrySet().stream()
                .map(e -> new BusinessDashboardResponse.DayRevenue(
                        e.getKey(), e.getValue(), paymentsPerDay.getOrDefault(e.getKey(), 0L)))
                .toList();
        List<BusinessDashboardResponse.TopProduct> products = top.values().stream()
                .sorted(Comparator.comparingLong((ProductCounter x) -> x.quantity).reversed()
                        .thenComparing(x -> x.title))
                .limit(5)
                .map(x -> new BusinessDashboardResponse.TopProduct(
                        x.id, x.title, x.quantity, x.completedOrders, x.revenue))
                .toList();
        long count = successfulPayments.size();
        BigDecimal averageCheck = count == 0 ? BigDecimal.ZERO :
                revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        // Ratio of COMPLETED order weight to SUCCESS-paid order weight in chosen period.
        // Clamp to 100: the sets can differ in time or due to mock/payment transitions.
        BigDecimal rescuedPercent = eligiblePaidWeight.signum() == 0 ? BigDecimal.ZERO :
                saved.multiply(BigDecimal.valueOf(100))
                        .divide(eligiblePaidWeight, 1, RoundingMode.HALF_UP)
                        .min(BigDecimal.valueOf(100));
        return new BusinessDashboardResponse(days, revenue, count, completed.size(),
                averageCheck, saved, eligiblePaidWeight, rescuedPercent,
                saved.multiply(CO2_PER_KG), series, products);
    }

    private static BigDecimal orderWeight(Order o) {
        if (o == null || o.getWeightKgPerUnitAtOrder() == null || o.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return o.getWeightKgPerUnitAtOrder().multiply(BigDecimal.valueOf(o.getQuantity()));
    }

    private static class ProductCounter {
        final UUID id;
        final String title;
        long quantity;
        long completedOrders;
        BigDecimal revenue = BigDecimal.ZERO;
        ProductCounter(UUID id, String title) { this.id = id; this.title = title; }
    }
}
