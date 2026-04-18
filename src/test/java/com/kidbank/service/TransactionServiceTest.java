package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Transaction;
import com.kidbank.model.Transaction.Type;
import com.kidbank.model.User;
import com.kidbank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock UserService userService;
    @InjectMocks TransactionService transactionService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("יובל").username("yuval")
                .checkingBalance(new BigDecimal("200.00")).build();
        when(userService.findUser(1L)).thenReturn(mockUser);
    }

    @Test
    void addIncome_increasesCheckingBalance() {
        Transaction saved = Transaction.builder().id(1L).user(mockUser).type(Type.INCOME)
                .amount(new BigDecimal("50.00")).description("דמי כיס").createdAt(LocalDateTime.now()).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse res = transactionService.addTransaction(1L,
                new TransactionRequest(Type.INCOME, new BigDecimal("50.00"), "דמי כיס"));

        assertThat(mockUser.getCheckingBalance()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(res.getType()).isEqualTo(Type.INCOME);
    }

    @Test
    void addTransaction_withCategory_persistsCategory() {
        Transaction saved = Transaction.builder().id(3L).user(mockUser).type(Type.INCOME)
                .amount(new BigDecimal("50.00")).description("מתנת יום הולדת")
                .category(Transaction.Category.BIRTHDAY).createdAt(LocalDateTime.now()).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse res = transactionService.addTransaction(1L,
                new TransactionRequest(Type.INCOME, new BigDecimal("50.00"), "מתנת יום הולדת", Transaction.Category.BIRTHDAY));

        assertThat(res.getCategory()).isEqualTo(Transaction.Category.BIRTHDAY);
    }

    @Test
    void addExpense_decreasesCheckingBalance() {
        Transaction saved = Transaction.builder().id(2L).user(mockUser).type(Type.EXPENSE)
                .amount(new BigDecimal("30.00")).description("פיצה").createdAt(LocalDateTime.now()).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        transactionService.addTransaction(1L,
                new TransactionRequest(Type.EXPENSE, new BigDecimal("30.00"), "פיצה"));

        assertThat(mockUser.getCheckingBalance()).isEqualByComparingTo(new BigDecimal("170.00"));
    }

    @Test
    void addExpense_insufficientBalance_throws() {
        assertThatThrownBy(() -> transactionService.addTransaction(1L,
                new TransactionRequest(Type.EXPENSE, new BigDecimal("999.00"), "יקר מדי")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void getTransactions_rangeMonth_filtersCorrectly() {
        Transaction tx = Transaction.builder().id(1L).user(mockUser).type(Type.INCOME)
                .amount(new BigDecimal("50.00")).description("test").createdAt(LocalDateTime.now()).build();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(List.of(tx));

        List<TransactionResponse> res = transactionService.getTransactions(1L, "month");

        assertThat(res).hasSize(1);
    }

    @Test
    void getTransactions_rangeAll_returnsAll() {
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<TransactionResponse> res = transactionService.getTransactions(1L, "all");

        assertThat(res).isEmpty();
        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getSummary_returnsCorrectTotals() {
        when(transactionRepository.sumByUserIdAndTypeAndCreatedAtAfter(eq(1L), eq(Type.INCOME), any()))
                .thenReturn(new BigDecimal("120.00"));
        when(transactionRepository.sumByUserIdAndTypeAndCreatedAtAfter(eq(1L), eq(Type.EXPENSE), any()))
                .thenReturn(new BigDecimal("47.00"));

        SummaryResponse res = transactionService.getSummary(1L, "month");

        assertThat(res.getTotalIncome()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(res.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("47.00"));
    }
}
