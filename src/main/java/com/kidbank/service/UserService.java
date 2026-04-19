package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Deposit;
import com.kidbank.model.User;
import com.kidbank.repository.DepositRepository;
import com.kidbank.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepositRepository depositRepository;

    public UserService(UserRepository userRepository, DepositRepository depositRepository) {
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
    }

    public UserResponse createUser(UserRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + req.getUsername());
        }
        User user = User.builder()
                .name(req.getName())
                .username(req.getUsername())
                .role(req.getRole() != null ? req.getRole() : com.kidbank.model.User.Role.KID)
                .build();
        user = userRepository.save(user);
        return toResponse(user, BigDecimal.ZERO);
    }

    public UserResponse getUser(Long userId) {
        User user = findUser(userId);
        BigDecimal depositTotal = depositRepository.findByUserId(userId)
                .map(Deposit::getTotalAmount)
                .orElse(BigDecimal.ZERO);
        return toResponse(user, depositTotal);
    }

    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        BigDecimal depositTotal = depositRepository.findByUserId(user.getId())
                .map(Deposit::getTotalAmount)
                .orElse(BigDecimal.ZERO);
        return toResponse(user, depositTotal);
    }

    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private UserResponse toResponse(User user, BigDecimal depositTotal) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .role(user.getRole())
                .checkingBalance(user.getCheckingBalance())
                .depositTotal(depositTotal)
                .totalBalance(user.getCheckingBalance().add(depositTotal))
                .build();
    }
}
