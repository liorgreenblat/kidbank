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
    public void verifyPin(@RequestBody PinRequest req) {
        if (!appSettingsService.verifyPin(req.getPin())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid PIN");
        }
    }

    @PutMapping("/parent-pin")
    public void updatePin(@RequestBody PinRequest req) {
        appSettingsService.updatePin(req.getPin());
    }
}
