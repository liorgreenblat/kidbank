package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.AppSettings;
import com.kidbank.model.Deposit;
import com.kidbank.repository.AppSettingsRepository;
import com.kidbank.repository.DepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AppSettingsService {

    private final AppSettingsRepository settingsRepository;
    private final DepositRepository depositRepository;

    public AppSettingsService(AppSettingsRepository settingsRepository, DepositRepository depositRepository) {
        this.settingsRepository = settingsRepository;
        this.depositRepository = depositRepository;
    }

    public AppSettings getOrCreate() {
        return settingsRepository.findById(1L).orElseGet(() ->
                settingsRepository.save(new AppSettings()));
    }

    public SettingsResponse getSettings() {
        return new SettingsResponse(getOrCreate().getGlobalInterestRate());
    }

    @Transactional
    public SettingsResponse updateInterestRate(InterestRateUpdateRequest req) {
        AppSettings settings = getOrCreate();
        settings.setGlobalInterestRate(req.getGlobalInterestRate());
        settingsRepository.save(settings);

        List<Deposit> allDeposits = depositRepository.findAll();
        allDeposits.forEach(d -> d.setInterestRate(req.getGlobalInterestRate()));
        depositRepository.saveAll(allDeposits);

        return new SettingsResponse(settings.getGlobalInterestRate());
    }

    public boolean verifyPin(String pin) {
        return getOrCreate().getParentPin().equals(pin);
    }

    @Transactional
    public void updatePin(String newPin) {
        if (newPin == null || !newPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
        AppSettings settings = getOrCreate();
        settings.setParentPin(newPin);
        settingsRepository.save(settings);
    }
}
