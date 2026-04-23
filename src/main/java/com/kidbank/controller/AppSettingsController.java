package com.kidbank.controller;

import com.kidbank.dto.Dtos.*;
import com.kidbank.service.AppSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class AppSettingsController {

    private final AppSettingsService appSettingsService;

    public AppSettingsController(AppSettingsService appSettingsService) {
        this.appSettingsService = appSettingsService;
    }

    @GetMapping
    public SettingsResponse getSettings() {
        return appSettingsService.getSettings();
    }

    @PutMapping("/interest-rate")
    public SettingsResponse updateInterestRate(@Valid @RequestBody InterestRateUpdateRequest req) {
        return appSettingsService.updateInterestRate(req);
    }

    @PostMapping("/verify-pin")
    public java.util.Map<String, Boolean> verifyPin(@RequestBody PinRequest req) {
        if (!appSettingsService.verifyPin(req.getPin())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid PIN");
        }
        return java.util.Map.of("valid", true);
    }

    @PutMapping("/parent-pin")
    public java.util.Map<String, Boolean> updatePin(@RequestBody PinRequest req) {
        appSettingsService.updatePin(req.getPin());
        return java.util.Map.of("ok", true);
    }
}
