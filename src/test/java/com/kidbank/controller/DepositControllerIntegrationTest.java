package com.kidbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Transaction.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DepositControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private Long userId;

    @BeforeEach
    void setup() throws Exception {
        // Create user
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("יובל", "yuval_dep"))))
                .andReturn().getResponse().getContentAsString();
        userId = objectMapper.readTree(body).get("id").asLong();

        // Give some checking balance
        mockMvc.perform(post("/api/users/" + userId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new TransactionRequest(Type.INCOME, new BigDecimal("200.00"), "הכנסה"))));
    }

    @Test
    void getDeposit_initiallyZero() throws Exception {
        mockMvc.perform(get("/api/users/" + userId + "/deposit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.interestRate").value(0.12))
                .andExpect(jsonPath("$.projectedOneMonth").value(0))
                .andExpect(jsonPath("$.projectedSixMonths").value(0))
                .andExpect(jsonPath("$.projectedOneYear").value(0));
    }

    @Test
    void addToDeposit_movesMoneyAndReturnsProjections() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/deposit/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DepositAmountRequest(new BigDecimal("80.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(80.00))
                .andExpect(jsonPath("$.projectedOneMonth").value(80.80))
                .andExpect(jsonPath("$.projectedSixMonths").value(84.80))
                .andExpect(jsonPath("$.projectedOneYear").value(89.60));

        // Checking balance should drop
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(jsonPath("$.checkingBalance").value(120.00))
                .andExpect(jsonPath("$.depositTotal").value(80.00))
                .andExpect(jsonPath("$.totalBalance").value(200.00));
    }

    @Test
    void addToDeposit_insufficientChecking_returns400() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/deposit/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DepositAmountRequest(new BigDecimal("9999.00")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawFromDeposit_movesMoneyBack() throws Exception {
        // First deposit
        mockMvc.perform(post("/api/users/" + userId + "/deposit/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepositAmountRequest(new BigDecimal("80.00")))));

        // Then withdraw
        mockMvc.perform(post("/api/users/" + userId + "/deposit/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DepositAmountRequest(new BigDecimal("30.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(50.00));

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(jsonPath("$.checkingBalance").value(150.00));
    }

    @Test
    void withdrawFromDeposit_moreThanDeposit_returns400() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/deposit/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepositAmountRequest(new BigDecimal("50.00")))));

        mockMvc.perform(post("/api/users/" + userId + "/deposit/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DepositAmountRequest(new BigDecimal("200.00")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setInterestRate_parentCanChange() throws Exception {
        mockMvc.perform(put("/api/users/" + userId + "/deposit/interest-rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InterestRateRequest(new BigDecimal("0.08")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestRate").value(0.08));
    }

    @Test
    void setInterestRate_above100percent_returns400() throws Exception {
        mockMvc.perform(put("/api/users/" + userId + "/deposit/interest-rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InterestRateRequest(new BigDecimal("1.5")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMultipleTimes_accumulatesCorrectly() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/deposit/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepositAmountRequest(new BigDecimal("50.00")))));

        mockMvc.perform(post("/api/users/" + userId + "/deposit/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositAmountRequest(new BigDecimal("30.00")))))
                .andExpect(jsonPath("$.totalAmount").value(80.00));
    }
}
