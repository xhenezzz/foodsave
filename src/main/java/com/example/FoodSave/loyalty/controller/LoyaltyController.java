package com.example.FoodSave.loyalty.controller;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.loyalty.LoyaltyService;
import com.example.FoodSave.loyalty.dto.LoyaltyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;

    /** Репутация только текущего авторизованного пользователя. */
    @GetMapping("/me")
    public LoyaltyResponse myReputation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
        return loyaltyService.getReputation(user);
    }
}
