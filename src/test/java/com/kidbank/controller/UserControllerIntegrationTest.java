package com.kidbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidbank.dto.Dtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createUser_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("יובל", "yuval_test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("יובל"))
                .andExpect(jsonPath("$.username").value("yuval_test"))
                .andExpect(jsonPath("$.checkingBalance").value(0))
                .andExpect(jsonPath("$.totalBalance").value(0));
    }

    @Test
    void createUser_duplicateUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("יובל", "dup_user"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("יובל 2", "dup_user"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no_name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_notFound_returns400() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_returnsCorrectBalance() throws Exception {
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("נועה", "noa_test"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.checkingBalance").value(0));
    }
}
