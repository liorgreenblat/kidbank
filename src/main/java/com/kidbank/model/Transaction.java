package com.kidbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum Type {
        INCOME, EXPENSE, DEPOSIT_IN, DEPOSIT_OUT
    }

    public enum Category {
        ALLOWANCE,      // דמי כיס
        BIRTHDAY,       // מתנת יום הולדת
        GIFT,           // מתנה
        FOOD,           // אוכל
        ENTERTAINMENT,  // בידור / משחקים
        BOOKS,          // ספרים
        TOYS,           // צעצועים
        SAVINGS,        // חסכונות / פיקדון
        OTHER           // אחר
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transaction() {}

    public Transaction(Long id, User user, Type type, BigDecimal amount, String description, Category category, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User user;
        private Type type;
        private BigDecimal amount;
        private String description;
        private Category category;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder type(Type type) { this.type = type; return this; }
        public Builder amount(BigDecimal a) { this.amount = a; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder category(Category c) { this.category = c; return this; }
        public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }
        public Transaction build() { return new Transaction(id, user, type, amount, description, category, createdAt); }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setType(Type type) { this.type = type; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public void setDescription(String d) { this.description = d; }
    public void setCategory(Category c) { this.category = c; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
