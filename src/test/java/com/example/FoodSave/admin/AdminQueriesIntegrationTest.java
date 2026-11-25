package com.example.FoodSave.admin;

import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs every admin JPQL query against the real PostgreSQL schema (same database as
 * FoodSaveApplicationTests). Read-only and rolled back: no data is created or changed.
 */
@SpringBootTest
@Transactional
class AdminQueriesIntegrationTest {
    @Autowired
    AdminService adminService;

    @Test
    void statsQueriesRunAndAreConsistent() {
        var stats = adminService.stats();
        assertEquals(stats.totalUsers(), stats.usersByRole().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(stats.totalOrders(), stats.ordersByStatus().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(AdminService.ACTIVITY_DAYS, stats.activity().size());
        assertTrue(stats.revenue().signum() >= 0);
        assertTrue(stats.foodSavedKg().signum() >= 0);
        assertTrue(stats.topBusinesses().size() <= 5);
    }

    @Test
    void listQueriesRunWithAndWithoutFilters() {
        assertNotNull(adminService.users(null, null));
        assertNotNull(adminService.users("a_%!", Role.CUSTOMER));
        assertNotNull(adminService.listings(null, null));
        assertNotNull(adminService.listings("хлеб", ListingStatus.ACTIVE));
        assertNotNull(adminService.orders(null));
        assertNotNull(adminService.orders(OrderStatus.COMPLETED));
        assertTrue(adminService.users(null, null).size() <= AdminService.MAX_ROWS);
    }
}
