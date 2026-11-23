
package com.example.FoodSave;

import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.loyalty.LoyaltyService;
import com.example.FoodSave.loyalty.ReputationLevel;
import com.example.FoodSave.loyalty.dto.LoyaltyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LoyaltyServiceTest {

    private LoyaltyService loyaltyService;
    private User user;

    private static final ZoneId ALMATY =
            ZoneId.of("Asia/Almaty");

    @BeforeEach
    void setUp() {
        loyaltyService = new LoyaltyService();

        user = new User();
        user.setFoodPoints(0);
        user.setTotalOrders(0);
        user.setCompletedOrders(0);
        user.setNoShowCount(0);
    }

    @Test
    void shouldAddFoodPoints() {
        loyaltyService.addPoints(user, 20);

        assertEquals(20, user.getFoodPoints());
    }

    @Test
    void shouldNotAllowNegativeFoodPoints() {
        user.setFoodPoints(3);

        loyaltyService.subtractPoints(user, 10);

        assertEquals(0, user.getFoodPoints());
    }

    @Test
    void shouldRewardCompletedOrder() {
        loyaltyService.orderCompleted(user);

        assertEquals(10, user.getFoodPoints());
        assertEquals(1, user.getCompletedOrders());
    }

    @Test
    void shouldPenalizeNoShow() {
        user.setFoodPoints(20);

        loyaltyService.orderNoShow(user);

        assertEquals(15, user.getFoodPoints());
        assertEquals(1, user.getNoShowCount());
    }

    @Test
    void shouldBlockAfterThreeNoShows() {
        LocalDateTime before =
                LocalDateTime.now(ALMATY);

        loyaltyService.orderNoShow(user);
        loyaltyService.orderNoShow(user);
        loyaltyService.orderNoShow(user);

        LocalDateTime after =
                LocalDateTime.now(ALMATY);

        assertEquals(3, user.getNoShowCount());

        assertNotNull(user.getBookingBlockedUntil());

        assertFalse(loyaltyService.canCreateOrder(user));

        assertFalse(
                user.getBookingBlockedUntil()
                        .isBefore(before.plusDays(7))
        );

        assertFalse(
                user.getBookingBlockedUntil()
                        .isAfter(after.plusDays(7))
        );
    }

    @Test
    void shouldNotBlockAfterTwoNoShows() {
        loyaltyService.orderNoShow(user);
        loyaltyService.orderNoShow(user);

        assertEquals(2, user.getNoShowCount());
        assertNull(user.getBookingBlockedUntil());
        assertTrue(loyaltyService.canCreateOrder(user));
    }

    @Test
    void shouldAllowOrdersWhenBlockExpired() {
        user.setBookingBlockedUntil(
                LocalDateTime.now(ALMATY).minusDays(1)
        );

        assertTrue(loyaltyService.canCreateOrder(user));
    }

    @Test
    void shouldRejectOrdersWhenBlockActive() {
        user.setBookingBlockedUntil(
                LocalDateTime.now(ALMATY).plusDays(7)
        );

        assertFalse(loyaltyService.canCreateOrder(user));
    }

    @Test
    void shouldBlockAgainAfterSixNoShows() {
        for (int i = 0; i < 3; i++) {
            loyaltyService.orderNoShow(user);
        }

        LocalDateTime firstBlock =
                user.getBookingBlockedUntil();

        for (int i = 0; i < 3; i++) {
            loyaltyService.orderNoShow(user);
        }

        assertEquals(6, user.getNoShowCount());

        assertEquals(
                firstBlock.plusDays(7),
                user.getBookingBlockedUntil()
        );
    }

    @Test
    void shouldCalculateReputation() {
        user.setCompletedOrders(9);
        user.setNoShowCount(1);
        user.setFoodPoints(85);

        LoyaltyResponse result =
                loyaltyService.getReputation(user);

        assertEquals(85, result.foodPoints());
        assertEquals(9, result.completedOrders());
        assertEquals(1, result.noShowCount());
        assertEquals(90, result.reliabilityPercent());
        assertEquals(
                ReputationLevel.BRONZE,
                result.reputationLevel()
        );
    }
}
