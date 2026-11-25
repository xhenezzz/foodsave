package com.example.FoodSave.business.service;

import com.example.FoodSave.auth.cfg.CurrentUser;
import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.business.ai.BusinessApplicationReviewer;
import com.example.FoodSave.business.dto.BusinessProfileRequest;
import com.example.FoodSave.business.dto.BusinessProfileResponse;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import com.example.FoodSave.moderation.ModerationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Business profile = business application. A new profile (and a resubmission after
 * rejection) waits for an administrator; the AI check only adds a recommendation.
 */
@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private final BusinessApplicationReviewer applicationReviewer;

    // Same centres and radius the frontend uses to decide a listing's city.
    private static final double MAX_CITY_RADIUS_KM = 90.0;
    private static final Map<City, double[]> CITY_CENTERS = Map.of(
            City.ALMATY, new double[]{43.238949, 76.889709},
            City.ASTANA, new double[]{51.169392, 71.449074});

    /** Submits a new business application. */
    @Transactional
    public BusinessProfileResponse createProfile(BusinessProfileRequest request) {
        User user = userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        if (user.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Заявку может подать только бизнес-аккаунт");
        }
        if (businessProfileRepository.existsByUserId(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Заявка уже подана — её можно отредактировать");
        }
        BusinessProfile profile = new BusinessProfile();
        profile.setUser(user);
        apply(profile, request);
        submitForReview(profile);
        return toResponse(businessProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public BusinessProfileResponse getMyProfile() {
        return toResponse(myProfile());
    }

    /**
     * Edits the profile. A pending or rejected application is (re)submitted for review;
     * an approved business keeps working — its edits are not re-moderated.
     */
    @Transactional
    public BusinessProfileResponse updateProfile(BusinessProfileRequest request) {
        BusinessProfile profile = myProfile();
        apply(profile, request);
        if (profile.getStatus() != ModerationStatus.APPROVED) {
            submitForReview(profile);
        }
        return toResponse(businessProfileRepository.save(profile));
    }

    private BusinessProfile myProfile() {
        return businessProfileRepository.findByUserId(CurrentUser.get().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Профиль бизнеса не найден"));
    }

    private void apply(BusinessProfile profile, BusinessProfileRequest request) {
        City city = profile.getUser().getCity();
        if (city == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У пользователя не указан город");
        }
        profile.setBusinessName(request.getBusinessName().trim());
        profile.setBusinessType(request.getBusinessType());
        profile.setDescription(blankToNull(request.getDescription()));
        profile.setAddress(request.getAddress().trim());
        profile.setContactPhone(request.getContactPhone().trim());
        profile.setBin(blankToNull(request.getBin()));
        GeocodingService.Coordinates coordinates = resolveCoordinates(
                city, request.getAddress(), request.getLatitude(), request.getLongitude());
        profile.setLatitude(coordinates.latitude());
        profile.setLongitude(coordinates.longitude());
    }

    private void submitForReview(BusinessProfile profile) {
        profile.setStatus(ModerationStatus.PENDING);
        profile.setRejectionReason(null);
        profile.setReviewedAt(null);
        profile.setSubmittedAt(LocalDateTime.now(ZoneId.of("Asia/Almaty")));
        BusinessApplicationReviewer.Review review = applicationReviewer.review(profile);
        profile.setAiVerdict(review.verdict());
        profile.setAiNotes(review.notesText());
    }

    /**
     * The business owner marks the exact spot on the map; without a point the address
     * is geocoded. The point must lie inside the account's city.
     */
    private GeocodingService.Coordinates resolveCoordinates(
            City city, String address, Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return geocodingService.geocode(city, address);
        }
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажи обе координаты метки на карте");
        }
        double[] center = CITY_CENTERS.get(city);
        if (center != null && distanceKm(latitude, longitude, center[0], center[1]) > MAX_CITY_RADIUS_KM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Метка на карте должна находиться в пределах города " + city.getDisplayName());
        }
        return new GeocodingService.Coordinates(latitude, longitude);
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BusinessProfileResponse toResponse(BusinessProfile profile) {
        return new BusinessProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getBusinessName(),
                profile.getDescription(),
                profile.getAddress(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getBusinessType(),
                profile.getContactPhone(),
                profile.getBin(),
                profile.getStatus(),
                profile.getRejectionReason(),
                profile.getSubmittedAt(),
                profile.getReviewedAt());
    }
}
