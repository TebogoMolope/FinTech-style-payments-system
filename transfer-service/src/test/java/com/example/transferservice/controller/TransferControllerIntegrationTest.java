package com.example.transferservice.controller;

import com.example.transferservice.repository.IdempotencyKeyRepository;
import com.example.transferservice.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ledger.service.base-url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
    }

    @Test
    void createTransfer_shouldSucceedOnHappyPath() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.post("/ledger/transfer")
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> request = new HashMap<>();
        request.put("fromAccountId", 1L);
        request.put("toAccountId", 2L);
        request.put("amount", 100.00);

        // When & Then
        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void createTransfer_shouldBeIdempotent() throws Exception {
        // Given
        wireMockServer.stubFor(WireMock.post("/ledger/transfer")
                .willReturn(aResponse().withStatus(200)));

        UUID idempotencyKey = UUID.randomUUID();
        Map<String, Object> request = new HashMap<>();
        request.put("fromAccountId", 1L);
        request.put("toAccountId", 2L);
        request.put("amount", 50.00);

        // First call
        String firstResponse = mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andReturn().getResponse().getContentAsString();

        // Second call
        String secondResponse = mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        // Then
        assertEquals(firstResponse, secondResponse);
    }
}
