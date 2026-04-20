package com.kidbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    public enum Role { PARENT, KID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal checkingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'KID'")
    private Role role = Role.KID;

    public User() {}

    public User(Long id, String name, String username, BigDecimal checkingBalance, Role role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.checkingBalance = checkingBalance;
        this.role = role;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String username;
        private BigDecimal checkingBalance = BigDecimal.ZERO;
        private Role role = Role.KID;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder checkingBalance(BigDecimal b) { this.checkingBalance = b; return this; }
        public Builder role(Role r) { this.role = r; return this; }
        public User build() { return new User(id, name, username, checkingBalance, role); }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public BigDecimal getCheckingBalance() { return checkingBalance; }
    public Role getRole() { return role; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUsername(String username) { this.username = username; }
    public void setCheckingBalance(BigDecimal b) { this.checkingBalance = b; }
    public void setRole(Role r) { this.role = r; }
}
