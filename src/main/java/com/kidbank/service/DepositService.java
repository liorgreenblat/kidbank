package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Deposit;
import com.kidbank.model.Transaction;
import com.kidbank.model.User;
import com.kidbank.repository.DepositRepository;
import com.kidbank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DepositService {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.12");

    private final DepositRepository depositRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public DepositService(DepositRepository depositRepository,
                          TransactionRepository transactionRepository,
                          UserService userService) {
        this.depositRepository = depositRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    public DepositResponse getDeposit(Long userId) {
        userService.findUser(userId);
        Deposit deposit = getOrCreate(userId);
        return toResponse(deposit);
    }

    @Transactional
    public DepositResponse addToDeposit(Long userId, DepositAmountRequest req) {
        User user = userService.findUser(userId);

        if (user.getCheckingBalance().compareTo(req.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient checking balance");
        }

        user.setCheckingBalance(user.getCheckingBalance().subtract(req.getAmount()));

        Deposit deposit = getOrCreate(userId);
        deposit.setTotalAmount(deposit.getTotalAmount().add(req.getAmount()));
        depositRepository.save(deposit);

        transactionRepository.save(Transaction.builder()
                .user(user)
                .type(Transaction.Type.DEPOSIT_IN)
                .amount(req.getAmount())
                .description("הפקדה לפיקדון")
                .category(Transaction.Category.SAVINGS)
                .build());

        return toResponse(deposit);
    }

    @Transactional
    public DepositResponse withdrawFromDeposit(Long userId, DepositAmountRequest req) {
        User user = userService.findUser(userId);
        Deposit deposit = depositRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No deposit found for user"));

        if (deposit.getTotalAmount().compareTo(req.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient deposit balance");
        }

        deposit.setTotalAmount(deposit.getTotalAmount().subtract(req.getAmount()));
        user.setCheckingBalance(user.getCheckingBalance().add(req.getAmount()));
        depositRepository.save(deposit);

        transactionRepository.save(Transaction.builder()
                .user(user)
                .type(Transaction.Type.DEPOSIT_OUT)
                .amount(req.getAmount())
                .description("משיכה מפיקדון")
                .category(Transaction.Category.SAVINGS)
                .build());

        return toResponse(deposit);
    }

    @Transactional
    public DepositResponse setInterestRate(Long userId, InterestRateRequest req) {
        userService.findUser(userId);
        Deposit deposit = getOrCreate(userId);
        deposit.setInterestRate(req.getInterestRate());
        depositRepository.save(deposit);
        return toResponse(deposit);
    }

    private Deposit getOrCreate(Long userId) {
        return depositRepository.findByUserId(userId).orElseGet(() -> {
            User user = userService.findUser(userId);
            return depositRepository.save(Deposit.builder()
                    .user(user)
                    .interestRate(DEFAULT_RATE)
                    .build());
        });
    }

    private BigDecimal project(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal t = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return principal.multiply(BigDecimal.ONE.add(annualRate.multiply(t)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private DepositResponse toResponse(Deposit d) {
        BigDecimal p = d.getTotalAmount();
        BigDecimal r = d.getInterestRate();
        return DepositResponse.builder()
                .id(d.getId())
                .totalAmount(p)
                .interestRate(r)
                .projectedOneMonth(project(p, r, 1))
                .projectedSixMonths(project(p, r, 6))
                .projectedOneYear(project(p, r, 12))
                .createdAt(d.getCreatedAt())
                .build();
    }
}
