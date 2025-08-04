package com.example.ledgerservice.controller;

import com.example.ledgerservice.domain.Account;
import com.example.ledgerservice.repository.AccountRepository;
import com.example.ledgerservice.repository.LedgerEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class LedgerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        accountRepository.deleteAll();
        fromAccount = accountRepository.save(new Account(null, new BigDecimal("1000.00"), null));
        toAccount = accountRepository.save(new Account(null, new BigDecimal("500.00"), null));
    }

    @AfterEach
    void tearDown() {
        log.info("--- Checking DB state after test ---");
        ledgerEntryRepository.findAll().forEach(entry -> log.info("Found entry: {}", entry));
        accountRepository.findAll().forEach(account -> log.info("Found account: {}", account));
        log.info("--- End of DB state check ---");
    }

    @Test
    void createAccount_shouldReturnCreatedAccount() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("initialBalance", "250.00");

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance", is(250.00)));
    }

    @Test
    void getAccount_shouldReturnAccountDetails() throws Exception {
        mockMvc.perform(get("/accounts/" + fromAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fromAccount.getId().intValue())))
                .andExpect(jsonPath("$.balance", is(1000.00)));
    }

    @Test
    void applyTransfer_shouldSucceedForValidTransfer() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("transferId", "tx-integration-1");
        request.put("fromAccountId", fromAccount.getId());
        request.put("toAccountId", toAccount.getId());
        request.put("amount", 100.00);

        mockMvc.perform(post("/ledger/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify balances
        mockMvc.perform(get("/accounts/" + fromAccount.getId()))
                .andExpect(jsonPath("$.balance", is(900.00)));
        mockMvc.perform(get("/accounts/" + toAccount.getId()))
                .andExpect(jsonPath("$.balance", is(600.00)));
    }

    @Test
    void applyTransfer_shouldFail_whenInsufficientFunds() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("transferId", "tx-integration-2");
        request.put("fromAccountId", fromAccount.getId());
        request.put("toAccountId", toAccount.getId());
        request.put("amount", 2000.00); // More than balance

        mockMvc.perform(post("/ledger/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyTransfer_shouldBeIdempotent() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("transferId", "tx-idempotent-1");
        request.put("fromAccountId", fromAccount.getId());
        request.put("toAccountId", toAccount.getId());
        request.put("amount", 50.00);

        // First call - should succeed
        mockMvc.perform(post("/ledger/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second call with same transferId - should also return OK but not re-process
        mockMvc.perform(post("/ledger/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify balances only changed once
        mockMvc.perform(get("/accounts/" + fromAccount.getId()))
                .andExpect(jsonPath("$.balance", is(950.00)));
        mockMvc.perform(get("/accounts/" + toAccount.getId()))
                .andExpect(jsonPath("$.balance", is(550.00)));
    }
}
