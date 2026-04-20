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
                .category(req.getCategory())
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

    @Transactional
    public TransactionResponse voidTransaction(Long userId, Long txId) {
        User user = userService.findUser(userId);
        Transaction original = transactionRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        if (!original.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Transaction does not belong to this user");
        }
        if (original.isVoided()) {
            throw new IllegalArgumentException("Transaction already voided");
        }

        // reverse the balance effect
        if (original.getType() == Type.INCOME) {
            if (user.getCheckingBalance().compareTo(original.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient balance to void this transaction");
            }
            user.setCheckingBalance(user.getCheckingBalance().subtract(original.getAmount()));
        } else if (original.getType() == Type.EXPENSE) {
            user.setCheckingBalance(user.getCheckingBalance().add(original.getAmount()));
        }

        // create counter-transaction
        Transaction counter = Transaction.builder()
                .user(user)
                .type(original.getType() == Type.INCOME ? Type.EXPENSE : Type.INCOME)
                .amount(original.getAmount())
                .description("תיקון: " + (original.getDescription() != null ? original.getDescription() : ""))
                .category(original.getCategory())
                .build();
        counter = transactionRepository.save(counter);

        original.setVoided(true);
        original.setVoidedBy(counter.getId());
        transactionRepository.save(original);

        return toResponse(original);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .category(tx.getCategory())
                .voided(tx.isVoided())
                .voidedBy(tx.getVoidedBy())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
