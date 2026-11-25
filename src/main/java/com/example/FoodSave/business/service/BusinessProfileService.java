package com.example.FoodSave.business.service;

import com.example.FoodSave.auth.cfg.CurrentUser;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.business.dto.BusinessProfileResponse;
import com.example.FoodSave.business.dto.CreateBusinessProfileRequest;
import com.example.FoodSave.business.dto.UpdateBusinessProfileRequest;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;

    // Same centres and radius the frontend uses to decide a listing's city.
    private static final double MAX_CITY_RADIUS_KM = 90.0;
    private static final Map<City, double[]> CITY_CENTERS = Map.of(
            City.ALMATY, new double[]{43.238949, 76.889709},
            City.ASTANA, new double[]{51.169392, 71.449074});

    @Transactional
    public BusinessProfileResponse createProfile(
            CreateBusinessProfileRequest request
    ) {

        UserPrincipal principal = CurrentUser.get();

        User user =
                userRepository.findById(principal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Пользователь не найден"
                                )
                        );

        if (user.getRole() != Role.BUSINESS) {
            throw new RuntimeException(
                    "Только BUSINESS может создать профиль бизнеса"
            );
        }

        if (businessProfileRepository.existsByUserId(user.getId())) {
            throw new RuntimeException(
                    "Профиль бизнеса уже существует"
            );
        }

        if (user.getCity() == null) {
            throw new RuntimeException(
                    "У пользователя не указан город"
            );
        }

        BusinessProfile profile =
                new BusinessProfile();

        profile.setUser(user);
        profile.setBusinessName(
                request.getBusinessName()
        );
        profile.setDescription(
                request.getDescription()
        );
        profile.setAddress(
                request.getAddress()
        );

        GeocodingService.Coordinates coordinates = resolveCoordinates(
                user.getCity(), request.getAddress(),
                request.getLatitude(), request.getLongitude());

        profile.setLatitude(
                coordinates.latitude()
        );

        profile.setLongitude(
                coordinates.longitude()
        );

        BusinessProfile saved =
                businessProfileRepository.save(profile);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BusinessProfileResponse getMyProfile() {

        UserPrincipal principal = CurrentUser.get();

        BusinessProfile profile =
                businessProfileRepository
                        .findByUserId(principal.getId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Профиль бизнеса не найден"
                                )
                        );

        return toResponse(profile);
    }

    @Transactional
    public BusinessProfileResponse updateProfile(
            UpdateBusinessProfileRequest request
    ) {

        UserPrincipal principal = CurrentUser.get();

        BusinessProfile profile =
                businessProfileRepository
                        .findByUserId(principal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Профиль бизнеса не найден"
                                )
                        );

        User user = profile.getUser();

        if (user.getCity() == null) {
            throw new RuntimeException(
                    "У пользователя не указан город"
            );
        }

        profile.setBusinessName(
                request.getBusinessName()
        );

        profile.setDescription(
                request.getDescription()
        );

        profile.setAddress(
                request.getAddress()
        );

        // Метка на карте приоритетнее геокодирования адреса. Город берём из User.
        GeocodingService.Coordinates coordinates = resolveCoordinates(
                user.getCity(), request.getAddress(),
                request.getLatitude(), request.getLongitude());

        profile.setLatitude(
                coordinates.latitude()
        );

        profile.setLongitude(
                coordinates.longitude()
        );

        BusinessProfile updated =
                businessProfileRepository.save(profile);

        return toResponse(updated);
    }

    /**
     * The business owner marks the exact spot on the map; that point used to be
     * silently discarded and replaced by Nominatim geocoding of the address.
     */
    private GeocodingService.Coordinates resolveCoordinates(
            City city, String address, Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return geocodingService.geocode(city, address);
        }
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Укажи обе координаты метки на карте");
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

    private BusinessProfileResponse toResponse(
            BusinessProfile profile
    ) {

        return new BusinessProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getBusinessName(),
                profile.getDescription(),
                profile.getAddress(),
                profile.getLatitude(),
                profile.getLongitude()
        );
    }
}