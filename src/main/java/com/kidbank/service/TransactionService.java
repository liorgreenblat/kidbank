package com.kidbank.service;

import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Transaction;
import com.kidbank.model.Transaction.Type;
import com.kidbank.model.User;
import com.kidbank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional
    public TransactionResponse addTransaction(Long userId, TransactionRequest req) {
        User user = userService.findUser(userId);

        if (req.getType() == Type.EXPENSE && user.getCheckingBalance().compareTo(req.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient checking balance");
        }

        if (req.getType() == Type.INCOME) {
            user.setCheckingBalance(user.getCheckingBalance().add(req.getAmount()));
        } else if (req.getType() == Type.EXPENSE) {
            user.setCheckingBalance(user.getCheckingBalance().subtract(req.getAmount()));
        }

        Transaction tx = Transaction.builder()
                .user(user)
                .type(req.getType())
                .amount(req.getAmount())
                .description(req.getDescription())
                .build();

        tx = transactionRepository.save(tx);
        return toResponse(tx);
    }

    public List<TransactionResponse> getTransactions(Long userId, String range) {
        userService.findUser(userId);
        LocalDateTime since = parseSince(range);
        List<Transaction> txs = since == null
                ? transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since);
        return txs.stream().map(this::toResponse).toList();
    }

    public SummaryResponse getSummary(Long userId, String range) {
        userService.findUser(userId);
        LocalDateTime since = parseSince(range);
        LocalDateTime effectiveSince = since == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : since;

        BigDecimal income = transactionRepository
                .sumByUserIdAndTypeAndCreatedAtAfter(userId, Type.INCOME, effectiveSince);
        BigDecimal expenses = transactionRepository
                .sumByUserIdAndTypeAndCreatedAtAfter(userId, Type.EXPENSE, effectiveSince);

        return SummaryResponse.builder()
                .totalIncome(income)
                .totalExpenses(expenses)
                .build();
    }

    private LocalDateTime parseSince(String range) {
        if (range == null) return null;
        return switch (range.toLowerCase()) {
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "half"  -> LocalDateTime.now().minusMonths(6);
            case "year"  -> LocalDateTime.now().minusYears(1);
            default      -> null;
        };
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
