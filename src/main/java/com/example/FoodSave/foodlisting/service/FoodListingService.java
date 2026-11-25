package com.example.FoodSave.foodlisting.service;

import com.example.FoodSave.auth.cfg.CurrentUser;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import com.example.FoodSave.foodlisting.dto.CreateFoodListingRequest;
import com.example.FoodSave.foodlisting.dto.FoodListingResponse;
import com.example.FoodSave.foodlisting.dto.UpdateFoodListingRequest;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.FoodCategory;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.moderation.ModerationStatus;
import com.example.FoodSave.review.repo.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodListingService {

    private final FoodListingRepository foodListingRepository;
    private final ReviewRepository reviewRepository;
    private final BusinessProfileRepository businessProfileRepository;

    public FoodListingResponse createListing(CreateFoodListingRequest request) {
        UserPrincipal userPrincipal = CurrentUser.get();
        BusinessProfile businessProfile = businessProfileRepository.findByUserId(userPrincipal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Профиль бизнеса не найден"));
        if (businessProfile.getStatus() != ModerationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Публиковать объявления можно после одобрения заявки бизнеса администратором");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Almaty"));
        ListingRules.validate(
                request.getTitle(), request.getDescription(),
                request.getOriginalPrice(), request.getFoodSavePrice(),
                request.getQuantity(), request.getWeightKg(),
                request.getPickupStart(), request.getPickupEnd(),
                request.getExpiresAt(), request.getProductExpiresAt(),
                now, false);
        ListingRules.validateNutrition(request.getCalories(), request.getProteins(), request.getFats(), request.getCarbs());
        if (request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Для нового предложения количество должно быть больше 0");
        }

        FoodListing listing = new FoodListing();
        listing.setTitle(request.getTitle().trim());
        listing.setDescription(request.getDescription());
        listing.setCategory(request.getCategory() == null ? FoodCategory.OTHER : request.getCategory());
        listing.setOriginalPrice(request.getOriginalPrice());
        listing.setFoodSavePrice(request.getFoodSavePrice());
        listing.setQuantity(request.getQuantity());
        listing.setWeightKg(request.getWeightKg());
        listing.setPickupStart(request.getPickupStart());
        listing.setPickupEnd(request.getPickupEnd());
        listing.setExpiresAt(request.getExpiresAt());
        listing.setProductExpiresAt(request.getProductExpiresAt());
        listing.setCalories(request.getCalories());
        listing.setProteins(request.getProteins());
        listing.setFats(request.getFats());
        listing.setCarbs(request.getCarbs());
        listing.setBusiness(businessProfile);
        listing.setStatus(ListingStatus.ACTIVE);
        // New offers are hidden from customers until an administrator approves them.
        listing.setModerationStatus(ModerationStatus.PENDING);

        return toResponse(foodListingRepository.save(listing));
    }

    /** Catalogue: /api/listings?city=ALMATY (city is optional). */
    @Transactional
    public List<FoodListingResponse> getActiveListings(String city) {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Almaty"));

        return foodListingRepository
                .findByStatus(ListingStatus.ACTIVE)
                .stream()
                .filter(listing -> isAvailable(listing, now))
                .filter(listing -> matchesCity(listing, city))
                .map(this::toResponse)
                .sorted(Comparator.comparingDouble(
                        FoodListingResponse::getRecommendationScore
                ).reversed())
                .toList();
    }

    public FoodListingResponse getListingById(
            UUID id
    ) {

        FoodListing listing =
                foodListingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Объявление не найдено"
                                )
                        );

        return toResponse(listing);
    }

    public List<FoodListingResponse> getMyListings() {

        UserPrincipal userPrincipal = CurrentUser.get();

        // User.id -> BusinessProfile. A business without a profile simply has no listings yet;
        // failing here broke the whole business dashboard for freshly registered accounts.
        BusinessProfile businessProfile =
                businessProfileRepository
                        .findByUserId(userPrincipal.getId())
                        .orElse(null);
        if (businessProfile == null) {
            return List.of();
        }

        return foodListingRepository
                .findByBusinessId(
                        businessProfile.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FoodListingResponse updateListing(
            UUID id,
            UpdateFoodListingRequest request
    ) {

        UserPrincipal userPrincipal = CurrentUser.get();

        // Same pessimistic row lock as createOrder(); protects available stock.
        FoodListing listing = foodListingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        if (!listing.getBusiness().getUser().getId().equals(userPrincipal.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Чужое объявление");
        }
        if (request.getVersion() == null || !request.getVersion().equals(listing.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Объявление изменилось. Обнови страницу и повтори редактирование");
        }
        if (listing.getStatus() == ListingStatus.CANCELED
                || listing.getStatus() == ListingStatus.EXPIRED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Объявление уже закрыто");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Almaty"));
        ListingRules.validate(
                request.getTitle(), request.getDescription(),
                request.getOriginalPrice(), request.getFoodSavePrice(),
                request.getQuantity(), request.getWeightKg(),
                request.getPickupStart(), request.getPickupEnd(),
                request.getExpiresAt(), request.getProductExpiresAt(), now,
                request.getPickupStart() != null
                        && request.getPickupStart().equals(listing.getPickupStart()));
        ListingRules.validateNutrition(request.getCalories(), request.getProteins(), request.getFats(), request.getCarbs());

        listing.setTitle(request.getTitle().trim());
        listing.setDescription(request.getDescription());
        if (request.getCategory() != null) listing.setCategory(request.getCategory());
        listing.setOriginalPrice(request.getOriginalPrice());
        listing.setFoodSavePrice(request.getFoodSavePrice());
        listing.setQuantity(request.getQuantity());
        listing.setWeightKg(request.getWeightKg());
        listing.setPickupStart(request.getPickupStart());
        listing.setPickupEnd(request.getPickupEnd());
        listing.setExpiresAt(request.getExpiresAt());
        listing.setProductExpiresAt(request.getProductExpiresAt());
        listing.setCalories(request.getCalories());
        listing.setProteins(request.getProteins());
        listing.setFats(request.getFats());
        listing.setCarbs(request.getCarbs());
        // A rejected offer goes back to the moderation queue once the business fixes it.
        // Edits of approved offers are not re-moderated, to keep the admin queue short.
        if (listing.getModerationStatus() == ModerationStatus.REJECTED) {
            listing.setModerationStatus(ModerationStatus.PENDING);
            listing.setModerationReason(null);
        }
        // Replenishing a SOLD_OUT listing makes it visible again, if dates are valid.
        if (listing.getQuantity() == 0) listing.setStatus(ListingStatus.SOLD_OUT);
        else if (listing.getStatus() == ListingStatus.SOLD_OUT) listing.setStatus(ListingStatus.ACTIVE);

        FoodListing updatedListing =
                foodListingRepository.save(listing);

        return toResponse(updatedListing);
    }

    @Transactional
    public void cancelListing(UUID id) {

        UserPrincipal userPrincipal = CurrentUser.get();

        FoodListing listing =
                foodListingRepository.findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Объявление не найдено"
                                )
                        );

        // Проверяем владельца
        if (!listing.getBusiness()
                .getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете удалить чужое объявление!"
            );
        }

        if (listing.getStatus()
                != ListingStatus.ACTIVE) {

            throw new RuntimeException(
                    "Это объявление уже нельзя отменить"
            );
        }

        listing.setStatus(
                ListingStatus.CANCELED
        );

        foodListingRepository.save(listing);
    }

    /** Listings within `radius` km: /api/listings/nearby?...&city=ALMATY (city is optional). */
    @Transactional
    public List<FoodListingResponse> getNearbyListings(
            double latitude, double longitude, double radius, String city) {

        if (!Double.isFinite(radius) || radius <= 0 || radius > 50) {
            throw new IllegalArgumentException("Радиус должен быть от 0 до 50 км");
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90 ||
                !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Недопустимые координаты");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Almaty"));

        return foodListingRepository
                .findNearbyListings(
                        latitude,
                        longitude,
                        radius
                )
                .stream()
                .filter(listing -> isAvailable(listing, now))
                .filter(listing -> matchesCity(listing, city))
                .map(listing -> toResponse(listing, distanceKm(latitude, longitude,
                        listing.getBusiness().getLatitude(), listing.getBusiness().getLongitude())))
                .sorted(Comparator.comparingDouble(
                        FoodListingResponse::getRecommendationScore
                ).reversed())
                .toList();
    }

    /** City is read from BusinessProfile -> User -> city; a blank filter matches everything. */
    private boolean matchesCity(FoodListing listing, String city) {
        if (city == null || city.isBlank()) return true;
        return listing.getBusiness() != null
                && listing.getBusiness().getUser() != null
                && listing.getBusiness().getUser().getCity() != null
                && listing.getBusiness().getUser().getCity().name().equalsIgnoreCase(city.trim());
    }

    /** Предложение доступно для бронирования в данный момент. */
    private boolean isAvailable(FoodListing listing, LocalDateTime now) {
        return listing.getStatus() == ListingStatus.ACTIVE
                && listing.getModerationStatus() == ModerationStatus.APPROVED
                && listing.getQuantity() != null
                && listing.getQuantity() > 0
                && listing.getExpiresAt() != null
                && listing.getExpiresAt().isAfter(now)
                && ListingRules.safeForPickup(listing.getProductExpiresAt(), now)
                && listing.getPickupEnd() != null
                && listing.getPickupEnd().isAfter(now);
    }

    private FoodListingResponse toResponse(FoodListing listing) {
        return toResponse(listing, null); // no buyer location -> distance is not scored
    }

    private FoodListingResponse toResponse(FoodListing listing, Double distanceKm) {
        BusinessProfile business = listing.getBusiness();
        Double average = reviewRepository.getAverageRating(business.getId());
        double rating = average == null ? 0.0 : average;

        double recommendationScore = RecommendationScorer.calculate(
                listing.getOriginalPrice(),
                listing.getFoodSavePrice(),
                listing.getQuantity(),
                rating,
                distanceKm);

        return new FoodListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getOriginalPrice(),
                listing.getFoodSavePrice(),
                listing.getQuantity(),
                listing.getWeightKg(),
                listing.getVersion(),
                listing.getPickupStart(),
                listing.getPickupEnd(),
                listing.getExpiresAt(),
                listing.getProductExpiresAt(),
                listing.getStatus(),
                business.getBusinessName(),
                business.getUser().getCity() == null ? null : business.getUser().getCity().name(),
                Math.round(rating * 10.0) / 10.0,
                distanceKm == null ? null : Math.round(distanceKm * 10.0) / 10.0,
                recommendationScore,
                business.getLatitude(),
                business.getLongitude(),
                listing.getCategory(),
                listing.getModerationStatus(),
                listing.getModerationReason(),
                listing.getCalories(),
                listing.getProteins(),
                listing.getFats(),
                listing.getCarbs()
        );
    }

    /** Great-circle distance (haversine), km. */
    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
