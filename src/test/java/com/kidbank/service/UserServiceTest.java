package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Deposit;
import com.kidbank.model.User;
import com.kidbank.repository.DepositRepository;
import com.kidbank.repository.UserRepository;
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
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock DepositRepository depositRepository;
    @InjectMocks UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("יובל").username("yuval")
                .checkingBalance(new BigDecimal("100.00")).build();
    }

    @Test
    void createUser_success() {
        when(userRepository.findByUsername("yuval")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(mockUser);

        UserResponse res = userService.createUser(new UserRequest("יובל", "yuval"));

        assertThat(res.getName()).isEqualTo("יובל");
        assertThat(res.getCheckingBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(res.getDepositTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(res.getTotalBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void createUser_duplicateUsername_throws() {
        when(userRepository.findByUsername("yuval")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> userService.createUser(new UserRequest("יובל", "yuval")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getUser_includesDepositInTotal() {
        Deposit deposit = Deposit.builder().totalAmount(new BigDecimal("80.00"))
                .interestRate(new BigDecimal("0.12")).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(depositRepository.findByUserId(1L)).thenReturn(Optional.of(deposit));

        UserResponse res = userService.getUser(1L);

        assertThat(res.getDepositTotal()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(res.getTotalBalance()).isEqualByComparingTo(new BigDecimal("180.00"));
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
