package com.kidbank.controller;

import com.kidbank.dto.Dtos.*;
import com.kidbank.service.DepositService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/deposit")
public class DepositController {

    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping
    public DepositResponse getDeposit(@PathVariable Long userId) {
        return depositService.getDeposit(userId);
    }

    @PostMapping("/add")
    public DepositResponse addToDeposit(@PathVariable Long userId,
                                        @Valid @RequestBody DepositAmountRequest req) {
        return depositService.addToDeposit(userId, req);
    }

    @PostMapping("/withdraw")
    public DepositResponse withdrawFromDeposit(@PathVariable Long userId,
                                               @Valid @RequestBody DepositAmountRequest req) {
        return depositService.withdrawFromDeposit(userId, req);
    }

    @PutMapping("/interest-rate")
    public DepositResponse setInterestRate(@PathVariable Long userId,
                                           @Valid @RequestBody InterestRateRequest req) {
        return depositService.setInterestRate(userId, req);
    }
}
