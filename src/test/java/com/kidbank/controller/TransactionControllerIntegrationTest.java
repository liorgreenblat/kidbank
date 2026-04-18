package com.kidbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidbank.dto.Dtos.*;
import com.kidbank.model.Transaction;
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
class TransactionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private Long userId;

    @BeforeEach
    void createUser() throws Exception {
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("יובל", "yuval_tx"))))
                .andReturn().getResponse().getContentAsString();
        userId = objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void addIncome_updatesBalance() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionRequest(Type.INCOME, new BigDecimal("100.00"), "דמי כיס"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(100.00));

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(jsonPath("$.checkingBalance").value(100.00));
    }

    @Test
    void addTransaction_withCategory_returnsCategory() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionRequest(Type.INCOME, new BigDecimal("50.00"), "מתנת יום הולדת", Transaction.Category.BIRTHDAY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("BIRTHDAY"));
    }

    @Test
    void addExpense_afterIncome_updatesBalance() throws Exception {
        addIncomeTx(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/users/" + userId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionRequest(Type.EXPENSE, new BigDecimal("40.00"), "פיצה"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(jsonPath("$.checkingBalance").value(60.00));
    }

    @Test
    void addExpense_insufficientBalance_returns400() throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionRequest(Type.EXPENSE, new BigDecimal("50.00"), "יקר מדי"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactions_returnsHistory() throws Exception {
        addIncomeTx(new BigDecimal("50.00"));
        addIncomeTx(new BigDecimal("30.00"));

        mockMvc.perform(get("/api/users/" + userId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSummary_rangeMonth_returnsCorrectTotals() throws Exception {
        addIncomeTx(new BigDecimal("120.00"));

        mockMvc.perform(get("/api/users/" + userId + "/transactions/summary?range=month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(120.00))
                .andExpect(jsonPath("$.totalExpenses").value(0));
    }

    // ── helpers ───────────────────────────────────────────
    private void addIncomeTx(BigDecimal amount) throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new TransactionRequest(Type.INCOME, amount, "הכנסה"))));
    }
}
