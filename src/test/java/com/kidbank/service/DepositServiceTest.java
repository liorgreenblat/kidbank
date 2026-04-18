package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock DepositRepository depositRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserService userService;
    @InjectMocks DepositService depositService;

    private User mockUser;
    private Deposit mockDeposit;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("יובל").username("yuval")
                .checkingBalance(new BigDecimal("200.00")).build();
        mockDeposit = Deposit.builder().id(1L).user(mockUser)
                .totalAmount(new BigDecimal("80.00")).interestRate(new BigDecimal("0.12")).build();
        when(userService.findUser(1L)).thenReturn(mockUser);
    }

    @Test
    void getDeposit_returnsProjections() {
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(mockDeposit));
        when(depositRepository.save(any())).thenReturn(mockDeposit);

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
}
