package com.kidbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal checkingBalance = BigDecimal.ZERO;

    public User() {}

    public User(Long id, String name, String username, BigDecimal checkingBalance) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.checkingBalance = checkingBalance;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String username;
        private BigDecimal checkingBalance = BigDecimal.ZERO;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder checkingBalance(BigDecimal b) { this.checkingBalance = b; return this; }
        public User build() { return new User(id, name, username, checkingBalance); }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public BigDecimal getCheckingBalance() { return checkingBalance; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUsername(String username) { this.username = username; }
    public void setCheckingBalance(BigDecimal b) { this.checkingBalance = b; }
}
