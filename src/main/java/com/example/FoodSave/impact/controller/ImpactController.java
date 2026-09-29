package com.example.FoodSave.impact.controller;

import com.example.FoodSave.impact.dto.ImpactResponse;
import com.example.FoodSave.impact.service.ImpactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/impact")
@RequiredArgsConstructor
public class ImpactController {

    private final ImpactService impactService;

    @GetMapping("/me")
    public ResponseEntity<ImpactResponse> getMyImpact() {

        return ResponseEntity.ok(
                impactService.getMyImpact()
        );
    }
}