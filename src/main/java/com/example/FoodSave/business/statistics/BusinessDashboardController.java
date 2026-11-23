package com.example.FoodSave.business.statistics;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/business/statistics")
@RequiredArgsConstructor
public class BusinessDashboardController {
    private final UserRepository userRepository;
    private final BusinessDashboardService dashboardService;

    @GetMapping
    public BusinessDashboardResponse get(@RequestParam(defaultValue = "30") int days) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization required");
        }
        User user = userRepository.findById(principal.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Business role required");
        }
        if (days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be 7, 30 or 90");
        }
        return dashboardService.getDashboard(user.getId(), days);
    }
}
