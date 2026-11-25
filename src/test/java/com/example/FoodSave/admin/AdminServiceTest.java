package com.example.FoodSave.admin;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.business.entity.BusinessProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FoodListingRepository listings = mock(FoodListingRepository.class);
    private final AdminService service = new AdminService(entityManager, users, listings);
    private User admin;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        admin = user(Role.ADMIN);
        var principal = new UserPrincipal(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        TypedQuery<String> nameQuery = mock(TypedQuery.class);
        when(nameQuery.setParameter(anyString(), any())).thenReturn(nameQuery);
        when(nameQuery.getResultStream()).thenAnswer(i -> Stream.empty());
        when(entityManager.createQuery(anyString(), eq(String.class))).thenReturn(nameQuery);
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(listings.save(any(FoodListing.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blockingStoresTrimmedReasonAndUnblockingClearsIt() {
        User customer = user(Role.CUSTOMER);
        when(users.findByIdForUpdate(customer.getId())).thenReturn(Optional.of(customer));

        var blocked = service.setBlocked(customer.getId(), true, "  спам  ");
        assertTrue(blocked.blocked());
        assertEquals("спам", blocked.blockedReason());

        var unblocked = service.setBlocked(customer.getId(), false, "ignored");
        assertFalse(unblocked.blocked());
        assertNull(unblocked.blockedReason());
    }

    @Test
    void adminCannotBlockThemselves() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.setBlocked(admin.getId(), true, null));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void otherAdminsCannotBeBlocked() {
        User otherAdmin = user(Role.ADMIN);
        when(users.findByIdForUpdate(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.setBlocked(otherAdmin.getId(), true, null));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertFalse(otherAdmin.isBlocked());
    }

    @Test
    void unknownUserIsNotFound() {
        UUID id = UUID.randomUUID();
        when(users.findByIdForUpdate(id)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.setBlocked(id, true, null));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void moderationCancelsActiveListing() {
        FoodListing listing = listing(ListingStatus.ACTIVE);
        when(listings.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));
        assertEquals(ListingStatus.CANCELED, service.cancelListing(listing.getId()).status());
    }

    @Test
    void closedListingCannotBeCancelledAgain() {
        FoodListing listing = listing(ListingStatus.EXPIRED);
        when(listings.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));
        var error = assertThrows(ResponseStatusException.class, () -> service.cancelListing(listing.getId()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void searchPatternEscapesLikeWildcards() {
        assertNull(AdminService.likePattern("   "));
        assertEquals("%anna%", AdminService.likePattern(" Anna "));
        assertEquals("%50!%!_off!!%", AdminService.likePattern("50%_off!"));
    }

    private static User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(role.name().toLowerCase());
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setRole(role);
        return user;
    }

    private static FoodListing listing(ListingStatus status) {
        User owner = user(Role.BUSINESS);
        BusinessProfile business = new BusinessProfile();
        business.setId(UUID.randomUUID());
        business.setBusinessName("Пекарня");
        business.setUser(owner);
        FoodListing listing = new FoodListing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Круассаны");
        listing.setStatus(status);
        listing.setQuantity(3);
        listing.setBusiness(business);
        return listing;
    }
}
