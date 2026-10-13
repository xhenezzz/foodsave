package com.example.FoodSave.foodlisting.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import com.example.FoodSave.foodlisting.dto.CreateFoodListingRequest;
import com.example.FoodSave.foodlisting.dto.FoodListingResponse;
import com.example.FoodSave.foodlisting.dto.UpdateFoodListingRequest;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.review.repo.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public FoodListingResponse createListing(
            CreateFoodListingRequest request
    ) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        // Получаем BusinessProfile текущего пользователя
        BusinessProfile businessProfile =
                businessProfileRepository
                        .findByUserId(userPrincipal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Профиль бизнеса не найден"
                                )
                        );

        // Проверяем цену
        if (request.getOriginalPrice()
                .compareTo(request.getFoodSavePrice()) <= 0) {

            throw new RuntimeException(
                    "Цена FoodSave должна быть ниже обычной цены"
            );
        }

        // Проверяем количество
        if (request.getQuantity() == null
                || request.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Количество должно быть больше 0"
            );
        }

        // Проверяем вес
        if (request.getWeightKg() == null
                || request.getWeightKg()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Вес должен быть больше 0"
            );
        }

        // Проверяем цену FoodSave
        if (request.getFoodSavePrice()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Цена должна быть больше 0"
            );
        }

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneId.of("Asia/Almaty")
                );

        if (request.getPickupStart().isBefore(now)) {
            throw new RuntimeException(
                    "Время начала получения не может быть в прошлом"
            );
        }

        if (request.getPickupEnd().isBefore(now)) {
            throw new RuntimeException(
                    "Время окончания получения не может быть в прошлом"
            );
        }

        if (request.getExpiresAt().isBefore(now)) {
            throw new RuntimeException(
                    "Время истечения предложения не может быть в прошлом"
            );
        }

        if (request.getExpiresAt().isBefore(request.getPickupEnd())) {
            throw new RuntimeException(
                    "Время истечения предложения не может быть раньше окончания получения"
            );
        }

        // Проверяем время получения
        if (request.getPickupStart()
                .isAfter(request.getPickupEnd())) {

            throw new RuntimeException(
                    "Начало получения не может быть позже окончания"
            );
        }

        FoodListing listing = new FoodListing();

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());

        listing.setOriginalPrice(
                request.getOriginalPrice()
        );

        listing.setFoodSavePrice(
                request.getFoodSavePrice()
        );

        listing.setQuantity(
                request.getQuantity()
        );

        listing.setWeightKg(
                request.getWeightKg()
        );

        listing.setPickupStart(
                request.getPickupStart()
        );

        listing.setPickupEnd(
                request.getPickupEnd()
        );

        listing.setExpiresAt(
                request.getExpiresAt()
        );

        // Теперь здесь BusinessProfile, а не User
        listing.setBusiness(
                businessProfile
        );

        listing.setStatus(
                ListingStatus.ACTIVE
        );

        FoodListing savedListing =
                foodListingRepository.save(listing);

        return toResponse(savedListing);
    }

    /** Backward-compatible old endpoint: does not filter city. */
    public List<FoodListingResponse> getActiveListings() {
        return getActiveListings(null);
    }

    /** Pass city from controller: /api/listings?city=ALMATY. */
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

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        // User.id -> BusinessProfile
        BusinessProfile businessProfile =
                businessProfileRepository
                        .findByUserId(userPrincipal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Профиль бизнеса не найден"
                                )
                        );

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

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        FoodListing listing =
                foodListingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Объявление не найдено"
                                )
                        );

        // Проверяем владельца объявления
        if (!listing.getBusiness()
                .getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете изменять чужое объявление"
            );
        }

        // Проверяем цену
        if (request.getOriginalPrice()
                .compareTo(request.getFoodSavePrice()) <= 0) {

            throw new RuntimeException(
                    "Цена FoodSave должна быть ниже обычной цены"
            );
        }

        // Проверяем количество
        if (request.getQuantity() == null
                || request.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Количество должно быть больше 0"
            );
        }

        // Проверяем вес
        if (request.getWeightKg() == null
                || request.getWeightKg()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Вес должен быть больше 0"
            );
        }

        // Проверяем время
        if (request.getPickupStart()
                .isAfter(request.getPickupEnd())) {

            throw new RuntimeException(
                    "Начало получения не может быть позже окончания"
            );
        }

        listing.setTitle(
                request.getTitle()
        );

        listing.setDescription(
                request.getDescription()
        );

        listing.setOriginalPrice(
                request.getOriginalPrice()
        );

        listing.setFoodSavePrice(
                request.getFoodSavePrice()
        );

        listing.setQuantity(
                request.getQuantity()
        );

        listing.setWeightKg(
                request.getWeightKg()
        );

        listing.setPickupStart(
                request.getPickupStart()
        );

        listing.setPickupEnd(
                request.getPickupEnd()
        );

        listing.setExpiresAt(
                request.getExpiresAt()
        );

        FoodListing updatedListing =
                foodListingRepository.save(listing);

        return toResponse(updatedListing);
    }

    @Transactional
    public void cancelListing(UUID id) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        FoodListing listing =
                foodListingRepository.findById(id)
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

    /** Backward-compatible method. */
    public List<FoodListingResponse> getNearbyListings(
            double latitude, double longitude, double radius) {
        return getNearbyListings(latitude, longitude, radius, null);
    }

    /** Pass city from controller: /api/listings/nearby?...&city=ALMATY. */
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
                .map(listing ->
                        toNearbyResponse(
                                listing,
                                latitude,
                                longitude
                        )
                )
                .sorted(Comparator.comparingDouble(
                        FoodListingResponse::getRecommendationScore
                ).reversed())
                .toList();
    }

    /** City is read from BusinessProfile -> User -> city.
     * Null city means the legacy unfiltered method, for compatibility.
     * NOTE: controller must authorize CUSTOMER and optionally derive city from JWT,
     * rather than relying on a query parameter for any security-sensitive rule.
     */
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
                && listing.getQuantity() != null
                && listing.getQuantity() > 0
                && listing.getExpiresAt() != null
                && listing.getExpiresAt().isAfter(now)
                && listing.getPickupEnd() != null
                && listing.getPickupEnd().isAfter(now);
    }

    private FoodListingResponse toNearbyResponse(
            FoodListing listing,
            double userLatitude,
            double userLongitude
    ) {

        BusinessProfile businessProfile =
                listing.getBusiness();

        Double rating =
                reviewRepository.getAverageRating(
                        businessProfile.getId()
                );

        if (rating == null) {
            rating = 0.0;
        }

        double distance =
                calculateDistance(
                        userLatitude,
                        userLongitude,
                        businessProfile.getLatitude(),
                        businessProfile.getLongitude()
                );

        double recommendationScore = RecommendationScorer.calculate(
                listing.getOriginalPrice(),
                listing.getFoodSavePrice(),
                listing.getQuantity(),
                rating,
                distance
        );

        return new FoodListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getOriginalPrice(),
                listing.getFoodSavePrice(),
                listing.getQuantity(),
                listing.getPickupStart(),
                listing.getPickupEnd(),
                listing.getExpiresAt(),
                listing.getStatus(),
                businessProfile.getBusinessName(),
                businessProfile.getUser().getCity() == null ? null : businessProfile.getUser().getCity().name(),
                Math.round(rating * 10.0) / 10.0,
                Math.round(distance * 10.0) / 10.0,
                recommendationScore,
                businessProfile.getLatitude(),
                businessProfile.getLongitude()
        );
    }

    private double calculateDistance(
            double userLatitude,
            double userLongitude,
            double businessLatitude,
            double businessLongitude
    ) {

        final double earthRadius = 6371.0;

        double latDistance =
                Math.toRadians(
                        businessLatitude - userLatitude
                );

        double lonDistance =
                Math.toRadians(
                        businessLongitude - userLongitude
                );

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)

                        + Math.cos(
                        Math.toRadians(userLatitude)
                )
                        * Math.cos(
                        Math.toRadians(businessLatitude)
                )
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return earthRadius * c;
    }

    private FoodListingResponse toResponse(
            FoodListing listing
    ) {

        BusinessProfile businessProfile =
                listing.getBusiness();

        Double rating =
                reviewRepository.getAverageRating(
                        businessProfile.getId()
                );

        if (rating == null) {
            rating = 0.0;
        }

        double recommendationScore = RecommendationScorer.calculate(
                listing.getOriginalPrice(),
                listing.getFoodSavePrice(),
                listing.getQuantity(),
                rating,
                null // Нет местоположения покупателя -> не учитываем расстояние
        );

        return new FoodListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getOriginalPrice(),
                listing.getFoodSavePrice(),
                listing.getQuantity(),
                listing.getPickupStart(),
                listing.getPickupEnd(),
                listing.getExpiresAt(),
                listing.getStatus(),
                businessProfile.getBusinessName(),
                businessProfile.getUser().getCity() == null ? null : businessProfile.getUser().getCity().name(),
                Math.round(rating * 10.0) / 10.0,
                null,
                recommendationScore,
                businessProfile.getLatitude(),
                businessProfile.getLongitude()
        );
    }
}
