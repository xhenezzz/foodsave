package com.example.FoodSave.business.ai;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/business/ai-listing")
@RequiredArgsConstructor
public class AiListingController {
    private final AiListingService aiListingService;
    private final AiListingImageValidator imageValidator;
    private final UserRepository userRepository;

    /** Legacy text-only endpoint kept for older frontend builds. */
    @PostMapping("/suggest")
    public AiListingSuggestion suggest(@Valid @RequestBody AiListingRequest request) {
        requireBusinessUser();
        return aiListingService.suggest(request);
    }

    /** Analyze one product photo and return an editable draft; never publishes a listing. */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiListingDraft analyze(
            @RequestParam("image") MultipartFile image,
            @RequestParam(name = "language", defaultValue = "ru") String language,
            @RequestParam(name = "notes", required = false) String notes) {
        requireBusinessUser();
        var validated = imageValidator.validate(image);
        return aiListingService.analyze(validated.bytes(), validated.mimeType(), language, notes);
    }

    private User requireBusinessUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Необходим вход в систему");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
        if (user.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI Listing доступен только бизнесу");
        }
        return user;
    }
}
