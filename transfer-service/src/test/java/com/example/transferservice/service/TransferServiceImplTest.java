package com.example.transferservice.service;

import com.example.transferservice.client.LedgerServiceClient;
import com.example.transferservice.domain.IdempotencyKey;
import com.example.transferservice.domain.Transfer;
import com.example.transferservice.domain.TransferStatus;
import com.example.transferservice.dto.CreateTransferRequest;
import com.example.transferservice.dto.TransferView;
import com.example.transferservice.repository.IdempotencyKeyRepository;
import com.example.transferservice.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private LedgerServiceClient ledgerServiceClient;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    void createTransfer_shouldReturnNewTransfer_whenKeyIsNew() throws JsonProcessingException {
        // Given
        UUID idempotencyKey = UUID.randomUUID();
        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, BigDecimal.TEN);
        Transfer savedTransfer = new Transfer(UUID.randomUUID(), 1L, 2L, BigDecimal.TEN, TransferStatus.PROCESSING, LocalDateTime.now());
        Transfer completedTransfer = new Transfer(savedTransfer.getId(), 1L, 2L, BigDecimal.TEN, TransferStatus.COMPLETED, savedTransfer.getCreatedAt());
        TransferView expectedView = new TransferView(completedTransfer.getId(), 1L, 2L, BigDecimal.TEN, TransferStatus.COMPLETED, completedTransfer.getCreatedAt());


        when(idempotencyKeyRepository.findById(idempotencyKey)).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer, completedTransfer);
        when(ledgerServiceClient.postTransfer(any())).thenReturn(Mono.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        TransferView actualView = transferService.createTransfer(idempotencyKey, request);

        // Then
        assertEquals(expectedView.getId(), actualView.getId());
        assertEquals(TransferStatus.COMPLETED, actualView.getStatus());
    }

    @Test
    void createTransfer_shouldReturnExistingTransfer_whenKeyExists() throws JsonProcessingException {
        // Given
        UUID idempotencyKey = UUID.randomUUID();
        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, BigDecimal.TEN);
        TransferView storedView = new TransferView(UUID.randomUUID(), 1L, 2L, BigDecimal.TEN, TransferStatus.COMPLETED, LocalDateTime.now());
        IdempotencyKey existingKey = new IdempotencyKey(idempotencyKey, "{}", 200, LocalDateTime.now().plusHours(1));

        when(idempotencyKeyRepository.findById(idempotencyKey)).thenReturn(Optional.of(existingKey));
        when(objectMapper.readValue("{}", TransferView.class)).thenReturn(storedView);

        // When
        TransferView actualView = transferService.createTransfer(idempotencyKey, request);

        // Then
        assertEquals(storedView, actualView);
    }
}
