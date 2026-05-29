package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Deposit;
import com.kidbank.model.Transaction;
import com.kidbank.model.User;
import com.kidbank.repository.DepositRepository;
import com.kidbank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);
    private static final BigDecimal DAYS_PER_YEAR = BigDecimal.valueOf(365);

    private final DepositRepository depositRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final AppSettingsService appSettingsService;

    public DepositService(DepositRepository depositRepository,
                          TransactionRepository transactionRepository,
                          UserService userService,
                          AppSettingsService appSettingsService) {
        this.depositRepository = depositRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.appSettingsService = appSettingsService;
    }

    @Transactional
    public DepositResponse getDeposit(Long userId) {
        userService.findUser(userId);
        Deposit deposit = getOrCreate(userId);
        if (applyPendingInterest(deposit)) depositRepository.save(deposit);
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
        applyPendingInterest(deposit);
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

        applyPendingInterest(deposit);

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
            BigDecimal rate = appSettingsService.getOrCreate().getGlobalInterestRate();
            return depositRepository.save(Deposit.builder()
                    .user(user)
                    .interestRate(rate)
                    .build());
        });
    }

    private BigDecimal project(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal t = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return principal.multiply(BigDecimal.ONE.add(annualRate.multiply(t)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal projectDays(BigDecimal principal, BigDecimal annualRate, int days) {
        BigDecimal t = BigDecimal.valueOf(days).divide(DAYS_PER_YEAR, 10, RoundingMode.HALF_UP);
        return principal.multiply(BigDecimal.ONE.add(annualRate.multiply(t)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Compounds the daily interest for every day since {@code lastInterestDate}.
     * Returns true if the deposit was changed and needs saving.
     * Safe to call on every fetch — no-op if already up to date.
     */
    boolean applyPendingInterest(Deposit d) {
        LocalDate today = LocalDate.now();
        LocalDate last = d.getLastInterestDate();
        if (last == null) {
            d.setLastInterestDate(today);
            return true;
        }
        long days = ChronoUnit.DAYS.between(last, today);
        if (days <= 0) return false;
        if (d.getTotalAmount() == null || d.getTotalAmount().signum() <= 0
                || d.getInterestRate() == null || d.getInterestRate().signum() <= 0) {
            d.setLastInterestDate(today);
            return true;
        }
        BigDecimal dailyFactor = BigDecimal.ONE.add(
                d.getInterestRate().divide(DAYS_PER_YEAR, 12, RoundingMode.HALF_UP));
        BigDecimal factor = dailyFactor.pow((int) Math.min(days, 36500), MathContext.DECIMAL64);
        BigDecimal grown = d.getTotalAmount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        d.setTotalAmount(grown);
        d.setLastInterestDate(today);
        return true;
    }

    /**
     * Daily scheduled job — applies interest at 00:05 Asia/Jerusalem to keep
     * displayed balances accurate even if no kid opens the app.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Jerusalem")
    @Transactional
    public void accrueInterestForAllDeposits() {
        int updated = 0;
        for (Deposit d : depositRepository.findAll()) {
            if (applyPendingInterest(d)) {
                depositRepository.save(d);
                updated++;
            }
        }
        log.info("Daily interest job: {} deposit(s) updated", updated);
    }

    private DepositResponse toResponse(Deposit d) {
        BigDecimal p = d.getTotalAmount();
        BigDecimal r = d.getInterestRate();
        return DepositResponse.builder()
                .id(d.getId())
                .totalAmount(p)
                .interestRate(r)
                .projectedOneWeek(projectDays(p, r, 7))
                .projectedOneMonth(project(p, r, 1))
                .projectedSixMonths(project(p, r, 6))
                .projectedOneYear(project(p, r, 12))
                .createdAt(d.getCreatedAt())
                .build();
    }
}
