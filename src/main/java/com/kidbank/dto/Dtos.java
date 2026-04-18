package com.kidbank.dto;

import com.kidbank.model.Transaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Dtos {

    // ── User ──────────────────────────────────────────────
    public static class UserRequest {
        @NotBlank public String name;
        @NotBlank public String username;

        public UserRequest() {}
        public UserRequest(String name, String username) { this.name = name; this.username = username; }
        public String getName() { return name; }
        public String getUsername() { return username; }
        public void setName(String n) { this.name = n; }
        public void setUsername(String u) { this.username = u; }
    }

    public static class UserResponse {
        public Long id;
        public String name;
        public String username;
        public BigDecimal checkingBalance;
        public BigDecimal depositTotal;
        public BigDecimal totalBalance;

        public UserResponse() {}
        public UserResponse(Long id, String name, String username,
                            BigDecimal checkingBalance, BigDecimal depositTotal, BigDecimal totalBalance) {
            this.id = id; this.name = name; this.username = username;
            this.checkingBalance = checkingBalance; this.depositTotal = depositTotal; this.totalBalance = totalBalance;
        }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private Long id; private String name; private String username;
            private BigDecimal checkingBalance; private BigDecimal depositTotal; private BigDecimal totalBalance;
            public Builder id(Long id) { this.id = id; return this; }
            public Builder name(String n) { this.name = n; return this; }
            public Builder username(String u) { this.username = u; return this; }
            public Builder checkingBalance(BigDecimal b) { this.checkingBalance = b; return this; }
            public Builder depositTotal(BigDecimal b) { this.depositTotal = b; return this; }
            public Builder totalBalance(BigDecimal b) { this.totalBalance = b; return this; }
            public UserResponse build() { return new UserResponse(id, name, username, checkingBalance, depositTotal, totalBalance); }
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getUsername() { return username; }
        public BigDecimal getCheckingBalance() { return checkingBalance; }
        public BigDecimal getDepositTotal() { return depositTotal; }
        public BigDecimal getTotalBalance() { return totalBalance; }
    }

    // ── Transaction ───────────────────────────────────────
    public static class TransactionRequest {
        @NotNull public Transaction.Type type;
        @NotNull @Positive public BigDecimal amount;
        public String description;
        public Transaction.Category category;

        public TransactionRequest() {}
        public TransactionRequest(Transaction.Type type, BigDecimal amount, String description) {
            this.type = type; this.amount = amount; this.description = description;
        }
        public TransactionRequest(Transaction.Type type, BigDecimal amount, String description, Transaction.Category category) {
            this.type = type; this.amount = amount; this.description = description; this.category = category;
        }
        public Transaction.Type getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getDescription() { return description; }
        public Transaction.Category getCategory() { return category; }
        public void setType(Transaction.Type t) { this.type = t; }
        public void setAmount(BigDecimal a) { this.amount = a; }
        public void setDescription(String d) { this.description = d; }
        public void setCategory(Transaction.Category c) { this.category = c; }
    }

    public static class TransactionResponse {
        public Long id;
        public Transaction.Type type;
        public BigDecimal amount;
        public String description;
        public Transaction.Category category;
        public LocalDateTime createdAt;

        public TransactionResponse() {}
        public TransactionResponse(Long id, Transaction.Type type, BigDecimal amount, String description, Transaction.Category category, LocalDateTime createdAt) {
            this.id = id; this.type = type; this.amount = amount; this.description = description; this.category = category; this.createdAt = createdAt;
        }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private Long id; private Transaction.Type type; private BigDecimal amount;
            private String description; private Transaction.Category category; private LocalDateTime createdAt;
            public Builder id(Long id) { this.id = id; return this; }
            public Builder type(Transaction.Type t) { this.type = t; return this; }
            public Builder amount(BigDecimal a) { this.amount = a; return this; }
            public Builder description(String d) { this.description = d; return this; }
            public Builder category(Transaction.Category c) { this.category = c; return this; }
            public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }
            public TransactionResponse build() { return new TransactionResponse(id, type, amount, description, category, createdAt); }
        }
        public Long getId() { return id; }
        public Transaction.Type getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getDescription() { return description; }
        public Transaction.Category getCategory() { return category; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    // ── Deposit ───────────────────────────────────────────
    public static class DepositAmountRequest {
        @NotNull @Positive public BigDecimal amount;

        public DepositAmountRequest() {}
        public DepositAmountRequest(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal a) { this.amount = a; }
    }

    public static class DepositResponse {
        public Long id;
        public BigDecimal totalAmount;
        public BigDecimal interestRate;
        public BigDecimal projectedOneMonth;
        public BigDecimal projectedSixMonths;
        public BigDecimal projectedOneYear;
        public LocalDateTime createdAt;

        public DepositResponse() {}
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private Long id; private BigDecimal totalAmount; private BigDecimal interestRate;
            private BigDecimal projectedOneMonth; private BigDecimal projectedSixMonths;
            private BigDecimal projectedOneYear; private LocalDateTime createdAt;
            public Builder id(Long id) { this.id = id; return this; }
            public Builder totalAmount(BigDecimal a) { this.totalAmount = a; return this; }
            public Builder interestRate(BigDecimal r) { this.interestRate = r; return this; }
            public Builder projectedOneMonth(BigDecimal p) { this.projectedOneMonth = p; return this; }
            public Builder projectedSixMonths(BigDecimal p) { this.projectedSixMonths = p; return this; }
            public Builder projectedOneYear(BigDecimal p) { this.projectedOneYear = p; return this; }
            public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }
            public DepositResponse build() {
                DepositResponse r = new DepositResponse();
                r.id = id; r.totalAmount = totalAmount; r.interestRate = interestRate;
                r.projectedOneMonth = projectedOneMonth; r.projectedSixMonths = projectedSixMonths;
                r.projectedOneYear = projectedOneYear; r.createdAt = createdAt;
                return r;
            }
        }
        public Long getId() { return id; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getInterestRate() { return interestRate; }
        public BigDecimal getProjectedOneMonth() { return projectedOneMonth; }
        public BigDecimal getProjectedSixMonths() { return projectedSixMonths; }
        public BigDecimal getProjectedOneYear() { return projectedOneYear; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class InterestRateRequest {
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") public BigDecimal interestRate;

        public InterestRateRequest() {}
        public InterestRateRequest(BigDecimal interestRate) { this.interestRate = interestRate; }
        public BigDecimal getInterestRate() { return interestRate; }
        public void setInterestRate(BigDecimal r) { this.interestRate = r; }
    }

    public static class SummaryResponse {
        public BigDecimal totalIncome;
        public BigDecimal totalExpenses;

        public SummaryResponse() {}
        public SummaryResponse(BigDecimal totalIncome, BigDecimal totalExpenses) {
            this.totalIncome = totalIncome; this.totalExpenses = totalExpenses;
        }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private BigDecimal totalIncome; private BigDecimal totalExpenses;
            public Builder totalIncome(BigDecimal i) { this.totalIncome = i; return this; }
            public Builder totalExpenses(BigDecimal e) { this.totalExpenses = e; return this; }
            public SummaryResponse build() { return new SummaryResponse(totalIncome, totalExpenses); }
        }
        public BigDecimal getTotalIncome() { return totalIncome; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
    }
}
