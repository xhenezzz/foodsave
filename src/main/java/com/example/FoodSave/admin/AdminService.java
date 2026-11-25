package com.example.FoodSave.admin;

import com.example.FoodSave.admin.dto.AdminListingResponse;
import com.example.FoodSave.admin.dto.AdminOrderResponse;
import com.example.FoodSave.admin.dto.AdminStatsResponse;
import com.example.FoodSave.admin.dto.AdminUserResponse;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-mostly platform administration. Results are capped (MAX_ROWS) — enough for
 * a prototype dashboard; a production version would page through the data.
 */
@Service
@RequiredArgsConstructor
public class AdminService {
    static final int MAX_ROWS = 200;
    static final int ACTIVITY_DAYS = 14;
    private static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
    private static final BigDecimal CO2_PER_KG = new BigDecimal("2.5"); // estimate, same as impact stats

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final FoodListingRepository foodListingRepository;

    // ------------------------------------------------------------------ stats

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Map<String, Long> usersByRole = countBy("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role", Role.values());
        Map<String, Long> listingsByStatus = countBy(
                "SELECT l.status, COUNT(l) FROM FoodListing l GROUP BY l.status", ListingStatus.values());
        Map<String, Long> ordersByStatus = countBy(
                "SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status", OrderStatus.values());

        long blocked = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.blocked = true", Long.class).getSingleResult();

        Object[] paid = entityManager.createQuery(
                        "SELECT COALESCE(SUM(p.amount), 0), COUNT(p) FROM Payment p WHERE p.status = :status",
                        Object[].class)
                .setParameter("status", PaymentStatus.SUCCESS)
                .getSingleResult();
        BigDecimal revenue = toDecimal(paid[0]).setScale(2, RoundingMode.HALF_UP);
        long paidOrders = ((Number) paid[1]).longValue();

        BigDecimal foodSaved = toDecimal(entityManager.createQuery("""
                        SELECT COALESCE(SUM(o.weightKgPerUnitAtOrder * o.quantity), 0)
                        FROM Order o WHERE o.status = :status AND o.weightKgPerUnitAtOrder IS NOT NULL
                        """, Object.class)
                .setParameter("status", OrderStatus.COMPLETED)
                .getSingleResult()).setScale(2, RoundingMode.HALF_UP);

        return new AdminStatsResponse(
                sum(usersByRole), blocked, usersByRole,
                sum(listingsByStatus), listingsByStatus,
                sum(ordersByStatus), ordersByStatus,
                revenue, paidOrders,
                foodSaved, foodSaved.multiply(CO2_PER_KG).setScale(2, RoundingMode.HALF_UP),
                activity(), topBusinesses());
    }

    private List<AdminStatsResponse.DayPoint> activity() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate first = today.minusDays(ACTIVITY_DAYS - 1L);
        LocalDateTime start = first.atStartOfDay();

        Map<LocalDate, Long> orders = entityManager.createQuery(
                        "SELECT o.createdAt FROM Order o WHERE o.createdAt >= :start", LocalDateTime.class)
                .setParameter("start", start)
                .getResultList().stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

        Map<LocalDate, BigDecimal> revenue = new HashMap<>();
        entityManager.createQuery(
                        "SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt >= :start", Payment.class)
                .setParameter("status", PaymentStatus.SUCCESS)
                .setParameter("start", start)
                .getResultList()
                .forEach(p -> revenue.merge(p.getCreatedAt().toLocalDate(), p.getAmount(), BigDecimal::add));

        List<AdminStatsResponse.DayPoint> points = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            points.add(new AdminStatsResponse.DayPoint(day,
                    orders.getOrDefault(day, 0L),
                    revenue.getOrDefault(day, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }

    private List<AdminStatsResponse.TopBusiness> topBusinesses() {
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT b.businessName, u.city, COUNT(o), COALESCE(SUM(o.totalPrice), 0)
                        FROM Order o JOIN o.listing l JOIN l.business b JOIN b.user u
                        WHERE o.status = :status
                        GROUP BY b.id, b.businessName, u.city
                        ORDER BY COUNT(o) DESC
                        """, Object[].class)
                .setParameter("status", OrderStatus.COMPLETED)
                .setMaxResults(5)
                .getResultList();
        return rows.stream().map(r -> new AdminStatsResponse.TopBusiness(
                (String) r[0],
                r[1] == null ? null : r[1].toString(),
                ((Number) r[2]).longValue(),
                toDecimal(r[3]).setScale(2, RoundingMode.HALF_UP))).toList();
    }

    // ------------------------------------------------------------------ users

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users(String query, Role role) {
        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1 = 1");
        String pattern = likePattern(query);
        if (pattern != null) {
            jpql.append(" AND (LOWER(u.username) LIKE :q ESCAPE '!' OR LOWER(u.email) LIKE :q ESCAPE '!'"
                    + " OR u.phoneNumber LIKE :q ESCAPE '!')");
        }
        if (role != null) jpql.append(" AND u.role = :role");
        jpql.append(" ORDER BY u.username");

        TypedQuery<User> q = entityManager.createQuery(jpql.toString(), User.class).setMaxResults(MAX_ROWS);
        if (pattern != null) q.setParameter("q", pattern);
        if (role != null) q.setParameter("role", role);
        List<User> users = q.getResultList();

        Map<UUID, String> businessNames = users.isEmpty() ? Map.of() : entityManager.createQuery(
                        "SELECT b FROM BusinessProfile b WHERE b.user.id IN :ids", BusinessProfile.class)
                .setParameter("ids", users.stream().map(User::getId).toList())
                .getResultList().stream()
                .collect(Collectors.toMap(b -> b.getUser().getId(), BusinessProfile::getBusinessName, (a, b) -> a));

        return users.stream().map(u -> toUser(u, businessNames.get(u.getId()))).toList();
    }

    @Transactional
    public AdminUserResponse setBlocked(UUID userId, boolean blocked, String reason) {
        UUID adminId = currentAdminId();
        if (adminId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя заблокировать собственный аккаунт");
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Администратора нельзя заблокировать");
        }
        user.setBlocked(blocked);
        String trimmed = reason == null ? null : reason.trim();
        user.setBlockedReason(blocked && trimmed != null && !trimmed.isEmpty() ? trimmed : null);
        User saved = userRepository.save(user);
        String businessName = entityManager.createQuery(
                        "SELECT b.businessName FROM BusinessProfile b WHERE b.user.id = :id", String.class)
                .setParameter("id", userId)
                .getResultStream().findFirst().orElse(null);
        return toUser(saved, businessName);
    }

    // ------------------------------------------------------------------ listings

    @Transactional(readOnly = true)
    public List<AdminListingResponse> listings(String query, ListingStatus status) {
        StringBuilder jpql = new StringBuilder(
                "SELECT l FROM FoodListing l JOIN FETCH l.business b JOIN FETCH b.user u WHERE 1 = 1");
        String pattern = likePattern(query);
        if (pattern != null) {
            jpql.append(" AND (LOWER(l.title) LIKE :q ESCAPE '!' OR LOWER(b.businessName) LIKE :q ESCAPE '!')");
        }
        if (status != null) jpql.append(" AND l.status = :status");
        jpql.append(" ORDER BY l.pickupStart DESC");

        TypedQuery<FoodListing> q = entityManager.createQuery(jpql.toString(), FoodListing.class)
                .setMaxResults(MAX_ROWS);
        if (pattern != null) q.setParameter("q", pattern);
        if (status != null) q.setParameter("status", status);
        return q.getResultList().stream().map(this::toListing).toList();
    }

    /** Moderation: takes an offer off the catalogue. Existing orders are left as they are. */
    @Transactional
    public AdminListingResponse cancelListing(UUID listingId) {
        FoodListing listing = foodListingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Объявление не найдено"));
        if (listing.getStatus() != ListingStatus.ACTIVE && listing.getStatus() != ListingStatus.SOLD_OUT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Объявление уже закрыто");
        }
        listing.setStatus(ListingStatus.CANCELED);
        return toListing(foodListingRepository.save(listing));
    }

    // ------------------------------------------------------------------ orders

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> orders(OrderStatus status) {
        String jpql = "SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.listing l JOIN FETCH l.business"
                + (status != null ? " WHERE o.status = :status" : "")
                + " ORDER BY o.createdAt DESC";
        TypedQuery<Order> q = entityManager.createQuery(jpql, Order.class).setMaxResults(MAX_ROWS);
        if (status != null) q.setParameter("status", status);
        List<Order> orders = q.getResultList();

        Map<UUID, PaymentStatus> payments = orders.isEmpty() ? Map.of() : entityManager.createQuery(
                        "SELECT p FROM Payment p WHERE p.order.id IN :ids", Payment.class)
                .setParameter("ids", orders.stream().map(Order::getId).toList())
                .getResultList().stream()
                .collect(Collectors.toMap(p -> p.getOrder().getId(), Payment::getStatus, (a, b) -> a));

        return orders.stream().map(o -> new AdminOrderResponse(
                o.getId(),
                o.getUser().getUsername(),
                o.getUser().getEmail(),
                o.getListing().getTitle(),
                o.getListing().getBusiness().getBusinessName(),
                o.getQuantity(),
                o.getTotalPrice(),
                o.getStatus(),
                payments.get(o.getId()),
                o.getCreatedAt(),
                o.getPickupEnd())).toList();
    }

    // ------------------------------------------------------------------ helpers

    private <E extends Enum<E>> Map<String, Long> countBy(String jpql, E[] all) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (E value : all) counts.put(value.name(), 0L);
        for (Object[] row : entityManager.createQuery(jpql, Object[].class).getResultList()) {
            if (row[0] != null) counts.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private static long sum(Map<String, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }

    static String likePattern(String query) {
        if (query == null || query.isBlank()) return null;
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                // '!' is the LIKE escape character (see ESCAPE '!' in the queries).
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    private AdminUserResponse toUser(User u, String businessName) {
        Function<Integer, Integer> zero = v -> v == null ? 0 : v;
        return new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getPhoneNumber(), u.getCity(),
                u.getRole(), u.isEmailVerified(), u.isBlocked(), u.getBlockedReason(),
                zero.apply(u.getFoodPoints()), zero.apply(u.getTotalOrders()),
                zero.apply(u.getCompletedOrders()), zero.apply(u.getNoShowCount()), businessName);
    }

    private AdminListingResponse toListing(FoodListing l) {
        BusinessProfile b = l.getBusiness();
        return new AdminListingResponse(l.getId(), l.getTitle(), b.getBusinessName(), b.getUser().getEmail(),
                b.getUser().getCity() == null ? null : b.getUser().getCity().name(),
                l.getStatus(), l.getOriginalPrice(), l.getFoodSavePrice(),
                l.getQuantity() == null ? 0 : l.getQuantity(),
                l.getPickupStart(), l.getPickupEnd(), l.getExpiresAt());
    }

    private UUID currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        return principal.getId();
    }
}
