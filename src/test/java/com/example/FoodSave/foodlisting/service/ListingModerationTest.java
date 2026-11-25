package com.example.FoodSave.foodlisting.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import com.example.FoodSave.foodlisting.dto.CreateFoodListingRequest;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.moderation.ModerationStatus;
import com.example.FoodSave.review.repo.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ListingModerationTest {
    private final FoodListingRepository listings = mock(FoodListingRepository.class);
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final BusinessProfileRepository businesses = mock(BusinessProfileRepository.class);
    private final FoodListingService service = new FoodListingService(listings, reviews, businesses);
    private BusinessProfile business;

    @BeforeEach
    void setup() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setRole(Role.BUSINESS);
        owner.setCity(City.ALMATY);
        var principal = new UserPrincipal(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        business = new BusinessProfile();
        business.setId(UUID.randomUUID());
        business.setUser(owner);
        business.setBusinessName("Пекарня");
        when(businesses.findByUserId(owner.getId())).thenReturn(Optional.of(business));
        when(listings.save(any(FoodListing.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private static CreateFoodListingRequest request() {
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Almaty")).plusHours(1);
        var r = new CreateFoodListingRequest();
        r.setTitle("Круассаны");
        r.setOriginalPrice(new BigDecimal("1500"));
        r.setFoodSavePrice(new BigDecimal("600"));
        r.setQuantity(5);
        r.setWeightKg(new BigDecimal("0.3"));
        r.setPickupStart(start);
        r.setPickupEnd(start.plusHours(2));
        r.setExpiresAt(start.plusHours(2));
        r.setProductExpiresAt(start.plusHours(10));
        r.setCalories(new BigDecimal("406"));
        r.setProteins(new BigDecimal("8.2"));
        return r;
    }

    @Test
    void businessCannotPublishBeforeApproval() {
        business.setStatus(ModerationStatus.PENDING);
        var error = assertThrows(ResponseStatusException.class, () -> service.createListing(request()));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(listings, never()).save(any());
    }

    @Test
    void newListingWaitsForModerationAndKeepsNutrition() {
        business.setStatus(ModerationStatus.APPROVED);
        var created = service.createListing(request());
        assertEquals(ModerationStatus.PENDING, created.getModerationStatus());
        assertEquals(new BigDecimal("406"), created.getCalories());
        assertEquals(new BigDecimal("8.2"), created.getProteins());
    }

    @Test
    void catalogShowsOnlyApprovedListings() {
        business.setStatus(ModerationStatus.APPROVED);
        FoodListing approved = available("Одобрено", ModerationStatus.APPROVED);
        FoodListing pending = available("На модерации", ModerationStatus.PENDING);
        FoodListing rejected = available("Отклонено", ModerationStatus.REJECTED);
        when(listings.findByStatus(ListingStatus.ACTIVE)).thenReturn(List.of(approved, pending, rejected));

        var catalog = service.getActiveListings(null);
        assertEquals(List.of("Одобрено"), catalog.stream().map(l -> l.getTitle()).toList());
    }

    private FoodListing available(String title, ModerationStatus moderation) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Almaty"));
        FoodListing listing = new FoodListing();
        listing.setId(UUID.randomUUID());
        listing.setTitle(title);
        listing.setBusiness(business);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setModerationStatus(moderation);
        listing.setQuantity(3);
        listing.setOriginalPrice(new BigDecimal("1000"));
        listing.setFoodSavePrice(new BigDecimal("500"));
        listing.setPickupEnd(now.plusHours(2));
        listing.setExpiresAt(now.plusHours(2));
        listing.setProductExpiresAt(now.plusHours(8));
        return listing;
    }
}
