package com.kidbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal globalInterestRate = new BigDecimal("0.12");

    @Column(nullable = false, length = 4)
    private String parentPin = "1234";

    public AppSettings() {}

    public Long getId() { return id; }
    public BigDecimal getGlobalInterestRate() { return globalInterestRate; }
    public String getParentPin() { return parentPin; }
    public void setGlobalInterestRate(BigDecimal r) { this.globalInterestRate = r; }
    public void setParentPin(String pin) { this.parentPin = pin; }
}
