package com.example.FoodSave.business.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.business.dto.BusinessProfileResponse;
import com.example.FoodSave.business.dto.CreateBusinessProfileRequest;
import com.example.FoodSave.business.dto.UpdateBusinessProfileRequest;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;

    @Transactional
    public BusinessProfileResponse createProfile(
            CreateBusinessProfileRequest request
    ) {

        UserPrincipal principal =
                getCurrentUserPrincipal();

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

        GeocodingService.Coordinates coordinates =
                geocodingService.geocode(
                        user.getCity(),
                        request.getAddress()
                );

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

        UserPrincipal principal =
                getCurrentUserPrincipal();

        BusinessProfile profile =
                businessProfileRepository
                        .findByUserId(principal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Профиль бизнеса не найден"
                                )
                        );

        return toResponse(profile);
    }

    @Transactional
    public BusinessProfileResponse updateProfile(
            UpdateBusinessProfileRequest request
    ) {

        UserPrincipal principal =
                getCurrentUserPrincipal();

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

        /*
         * При изменении адреса заново получаем координаты.
         * Город снова берём из User.
         */
        GeocodingService.Coordinates coordinates =
                geocodingService.geocode(
                        user.getCity(),
                        request.getAddress()
                );

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

    private UserPrincipal getCurrentUserPrincipal() {

        return (UserPrincipal)
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();
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