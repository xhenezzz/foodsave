package com.example.FoodSave.loyalty;

import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.loyalty.dto.LoyaltyResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class LoyaltyService {
    public static final int COMPLETED_REWARD = 10;
    public static final int NO_SHOW_PENALTY = 5;
    public static final int NO_SHOWS_PER_BLOCK = 3;
    public static final int BLOCK_DAYS = 7;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Almaty");

    public void addPoints(User user, int points) {
        if (points < 0) throw new IllegalArgumentException("Баллы должны быть неотрицательными");
        user.setFoodPoints(Math.addExact(orZero(user.getFoodPoints()), points));
    }

    public void subtractPoints(User user, int points) {
        if (points < 0) throw new IllegalArgumentException("Штраф должен быть неотрицательным");
        user.setFoodPoints(Math.max(0, orZero(user.getFoodPoints()) - points));
    }

    public void orderCompleted(User user) {
        addPoints(user, COMPLETED_REWARD);
        user.setCompletedOrders(Math.addExact(orZero(user.getCompletedOrders()), 1));
    }

    public void orderNoShow(User user) {
        subtractPoints(user, NO_SHOW_PENALTY);
        int noShows = Math.addExact(orZero(user.getNoShowCount()), 1);
        user.setNoShowCount(noShows);

        if (noShows % NO_SHOWS_PER_BLOCK == 0) {
            LocalDateTime now = now();
            LocalDateTime blockedUntil = user.getBookingBlockedUntil();
            LocalDateTime blockFrom = blockedUntil != null && blockedUntil.isAfter(now)
                    ? blockedUntil : now;
            user.setBookingBlockedUntil(blockFrom.plusDays(BLOCK_DAYS));
        }
    }

    public boolean canCreateOrder(User user) {
        return canCreateOrderAt(user, now());
    }

    boolean canCreateOrderAt(User user, LocalDateTime now) {
        LocalDateTime until = user.getBookingBlockedUntil();
        return until == null || !until.isAfter(now);
    }

    public void orderCreated(User user) {
        user.setTotalOrders(Math.addExact(orZero(user.getTotalOrders()), 1));
    }

    public LoyaltyResponse getReputation(User user) {
        int completed = orZero(user.getCompletedOrders());
        int noShows = orZero(user.getNoShowCount());
        int resolved = completed + noShows;
        Integer reliabilityPercent = resolved == 0 ? null
                : (int) Math.round(100.0 * completed / resolved);
        boolean bookingAllowed = canCreateOrder(user);
        ReputationLevel level = level(completed, reliabilityPercent);

        return new LoyaltyResponse(
                user.getFsId(),
                orZero(user.getFoodPoints()),
                orZero(user.getTotalOrders()),
                completed,
                noShows,
                reliabilityPercent,
                level,
                bookingAllowed,
                bookingAllowed ? null : user.getBookingBlockedUntil(),
                NO_SHOWS_PER_BLOCK - noShows % NO_SHOWS_PER_BLOCK
        );
    }

    private ReputationLevel level(int completed, Integer reliabilityPercent) {
        if (reliabilityPercent == null) return ReputationLevel.NEW;
        if (completed >= 3 && reliabilityPercent < 70) return ReputationLevel.NEEDS_IMPROVEMENT;
        if (completed >= 25 && reliabilityPercent >= 95) return ReputationLevel.GOLD;
        if (completed >= 10 && reliabilityPercent >= 90) return ReputationLevel.SILVER;
        if (completed >= 3 && reliabilityPercent >= 80) return ReputationLevel.BRONZE;
        return ReputationLevel.NEW;
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(APP_ZONE);
    }
}
