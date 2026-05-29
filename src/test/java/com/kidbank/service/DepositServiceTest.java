package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.AppSettings;
import com.kidbank.model.Deposit;
import com.kidbank.model.User;
import com.kidbank.repository.DepositRepository;
import com.kidbank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock DepositRepository depositRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserService userService;
    @Mock AppSettingsService appSettingsService;
    @InjectMocks DepositService depositService;

    private User mockUser;
    private Deposit mockDeposit;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("יובל").username("yuval")
                .checkingBalance(new BigDecimal("200.00")).build();
        mockDeposit = Deposit.builder().id(1L).user(mockUser)
                .totalAmount(new BigDecimal("80.00")).interestRate(new BigDecimal("0.12")).build();
        lenient().when(userService.findUser(1L)).thenReturn(mockUser);
        AppSettings defaultSettings = new AppSettings();
        lenient().when(appSettingsService.getOrCreate()).thenReturn(defaultSettings);
    }

    @Test
    void getDeposit_returnsProjections() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));

        DepositResponse res = depositService.getDeposit(1L);

        assertThat(res.getTotalAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(res.getProjectedOneMonth()).isEqualByComparingTo(new BigDecimal("80.80"));
        assertThat(res.getProjectedSixMonths()).isEqualByComparingTo(new BigDecimal("84.80"));
        assertThat(res.getProjectedOneYear()).isEqualByComparingTo(new BigDecimal("89.60"));
    }

    @Test
    void addToDeposit_movesMoneyFromChecking() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));
        when(depositRepository.save(any())).thenReturn(mockDeposit);

        depositService.addToDeposit(1L, new DepositAmountRequest(new BigDecimal("50.00")));

        assertThat(mockUser.getCheckingBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(mockDeposit.getTotalAmount()).isEqualByComparingTo(new BigDecimal("130.00"));
        verify(transactionRepository).save(any());
    }

    @Test
    void addToDeposit_insufficientChecking_throws() {
        assertThatThrownBy(() ->
                depositService.addToDeposit(1L, new DepositAmountRequest(new BigDecimal("999.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient checking");
    }

    @Test
    void withdrawFromDeposit_movesMoneyToChecking() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));
        when(depositRepository.save(any())).thenReturn(mockDeposit);

        depositService.withdrawFromDeposit(1L, new DepositAmountRequest(new BigDecimal("30.00")));

        assertThat(mockDeposit.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(mockUser.getCheckingBalance()).isEqualByComparingTo(new BigDecimal("230.00"));
        verify(transactionRepository).save(any());
    }

    @Test
    void withdrawFromDeposit_insufficientDeposit_throws() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));

        assertThatThrownBy(() ->
                depositService.withdrawFromDeposit(1L, new DepositAmountRequest(new BigDecimal("999.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient deposit");
    }

    @Test
    void withdrawFromDeposit_noDeposit_throws() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                depositService.withdrawFromDeposit(1L, new DepositAmountRequest(new BigDecimal("10.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No deposit found");
    }

    @Test
    void setInterestRate_updatesRate() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));
        when(depositRepository.save(any())).thenReturn(mockDeposit);

        depositService.setInterestRate(1L, new InterestRateRequest(new BigDecimal("0.08")));

        assertThat(mockDeposit.getInterestRate()).isEqualByComparingTo(new BigDecimal("0.08"));
    }

    @Test
    void getDeposit_createsDefaultIfNotExists() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.empty());
        Deposit newDeposit = Deposit.builder().id(2L).user(mockUser)
                .totalAmount(BigDecimal.ZERO).interestRate(new BigDecimal("0.12")).build();
        when(depositRepository.save(any())).thenReturn(newDeposit);

        DepositResponse res = depositService.getDeposit(1L);

        assertThat(res.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(res.getInterestRate()).isEqualByComparingTo(new BigDecimal("0.12"));
    }

    @Test
    void getDeposit_includesOneWeekProjection() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));

        DepositResponse res = depositService.getDeposit(1L);

        // 80 * (1 + 0.12 * 7/365) ≈ 80.184
        assertThat(res.getProjectedOneWeek())
                .isCloseTo(new BigDecimal("80.18"), within(new BigDecimal("0.02")));
    }

    @Test
    void applyPendingInterest_compoundsBalanceForElapsedDays() {
        Deposit d = Deposit.builder().id(9L).user(mockUser)
                .totalAmount(new BigDecimal("100.00"))
                .interestRate(new BigDecimal("0.365"))  // 0.1% per day for easy math
                .build();
        d.setLastInterestDate(LocalDate.now().minusDays(10));

        boolean changed = depositService.applyPendingInterest(d);

        assertThat(changed).isTrue();
        // 100 * (1.001)^10 ≈ 101.0045
        assertThat(d.getTotalAmount())
                .isCloseTo(new BigDecimal("101.00"), within(new BigDecimal("0.05")));
        assertThat(d.getLastInterestDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void applyPendingInterest_noOpWhenAlreadyToday() {
        Deposit d = Deposit.builder().user(mockUser)
                .totalAmount(new BigDecimal("100.00"))
                .interestRate(new BigDecimal("0.12")).build();
        d.setLastInterestDate(LocalDate.now());

        boolean changed = depositService.applyPendingInterest(d);

        assertThat(changed).isFalse();
        assertThat(d.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void applyPendingInterest_zeroBalance_justBumpsDateNoGrowth() {
        Deposit d = Deposit.builder().user(mockUser)
                .totalAmount(BigDecimal.ZERO)
                .interestRate(new BigDecimal("0.12")).build();
        d.setLastInterestDate(LocalDate.now().minusDays(30));

        depositService.applyPendingInterest(d);

        assertThat(d.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(d.getLastInterestDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void accrueInterestForAllDeposits_savesUpdatedOnes() {
        Deposit stale = Deposit.builder().id(1L).user(mockUser)
                .totalAmount(new BigDecimal("100.00"))
                .interestRate(new BigDecimal("0.12")).build();
        stale.setLastInterestDate(LocalDate.now().minusDays(2));
        Deposit fresh = Deposit.builder().id(2L).user(mockUser)
                .totalAmount(new BigDecimal("50.00"))
                .interestRate(new BigDecimal("0.12")).build();
        fresh.setLastInterestDate(LocalDate.now());
        when(depositRepository.findAll()).thenReturn(List.of(stale, fresh));

        depositService.accrueInterestForAllDeposits();

        verify(depositRepository, times(1)).save(stale);
        verify(depositRepository, never()).save(fresh);
    }
}
