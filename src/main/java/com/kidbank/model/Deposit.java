package com.kidbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposits")
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Deposit() {}

    public Deposit(Long id, User user, BigDecimal totalAmount, BigDecimal interestRate, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.totalAmount = totalAmount;
        this.interestRate = interestRate;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User user;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal interestRate;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder totalAmount(BigDecimal a) { this.totalAmount = a; return this; }
        public Builder interestRate(BigDecimal r) { this.interestRate = r; return this; }
        public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }
        public Deposit build() { return new Deposit(id, user, totalAmount, interestRate, createdAt); }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setTotalAmount(BigDecimal a) { this.totalAmount = a; }
    public void setInterestRate(BigDecimal r) { this.interestRate = r; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
