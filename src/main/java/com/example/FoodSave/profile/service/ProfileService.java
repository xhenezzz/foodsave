package com.example.FoodSave.profile.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.profile.dto.ChangePasswordRequest;
import com.example.FoodSave.profile.dto.FoodSaveQrResponse;
import com.example.FoodSave.profile.dto.ProfileResponse;
import com.example.FoodSave.profile.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    private double calculateReliability(User user) {

        int completed = user.getCompletedOrders() == null
                ? 0
                : user.getCompletedOrders();

        int noShows = user.getNoShowCount() == null
                ? 0
                : user.getNoShowCount();

        int total = completed + noShows;

        if (total == 0) {
            return 100.0;
        }

        return Math.round(
                ((double) completed / total) * 1000
        ) / 10.0;
    }

    public ProfileResponse getMyProfile(){
        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = repository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return new ProfileResponse(
                user.getId(),
                user.getFsId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCity(),
                user.getRole(),
                user.getVerificationStatus(),
                user.getFoodPoints(),
                user.getCompletedOrders(),
                user.getNoShowCount(),
                calculateReliability(user)
        );
    }


    public ProfileResponse updateProfile(UpdateProfileRequest request) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = repository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

        if (!user.getEmail().equals(request.getEmail())
                && repository.existsByEmail(request.getEmail())) {

            throw new RuntimeException("Email уже занят");
        }

        if (!user.getPhoneNumber().equals(request.getPhoneNumber())
                && repository.existsByPhoneNumber(request.getPhoneNumber())) {

            throw new RuntimeException("Номер телефона уже занят");
        }

        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(request.getCity());

        User savedUser = repository.save(user);

        return new ProfileResponse(
                user.getId(),
                user.getFsId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCity(),
                user.getRole(),
                user.getVerificationStatus(),
                user.getFoodPoints(),
                user.getCompletedOrders(),
                user.getNoShowCount(),
                calculateReliability(user)
        );
    }

    public void changePassword(ChangePasswordRequest request) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = repository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Старый пароль указан неверно");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        repository.save(user);
    }

    public FoodSaveQrResponse getFoodSaveQr() {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = repository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

        String qrData =
                "http://localhost:5173/u/" + user.getFsId();

        return new FoodSaveQrResponse(
                user.getFsId(),
                qrData
        );
    }
}
