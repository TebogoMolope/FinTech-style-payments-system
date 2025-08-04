package com.example.transferservice.service;

import com.example.transferservice.client.LedgerServiceClient;
import com.example.transferservice.domain.IdempotencyKey;
import com.example.transferservice.domain.Transfer;
import com.example.transferservice.domain.TransferStatus;
import com.example.transferservice.dto.CreateTransferRequest;
import com.example.transferservice.dto.LedgerTransferRequest;
import com.example.transferservice.dto.TransferView;
import com.example.transferservice.exception.IdempotencyKeyConflictException;
import com.example.transferservice.repository.IdempotencyKeyRepository;
import com.example.transferservice.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final LedgerServiceClient ledgerServiceClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(20);

    @Override
    @Transactional
    public TransferView createTransfer(UUID idempotencyKey, CreateTransferRequest request) {
        Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findById(idempotencyKey);

        if (existingKeyOpt.isPresent()) {
            IdempotencyKey existingKey = existingKeyOpt.get();
            if (existingKey.getExpiryAt().isAfter(LocalDateTime.now())) {
                log.warn("Idempotent key {} already processed. Returning original response.", idempotencyKey);
                try {
                    return objectMapper.readValue(existingKey.getResponseBody(), TransferView.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Could not deserialize stored idempotent response", e);
                }
            } else {
                // Key expired, so we can process it as a new request. Delete the old key.
                idempotencyKeyRepository.delete(existingKey);
            }
        }

        // Create and save the initial transfer record
        Transfer transfer = Transfer.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status(TransferStatus.PROCESSING)
                .build();
        transfer = transferRepository.save(transfer);

        // Call the ledger service
        LedgerTransferRequest ledgerRequest = LedgerTransferRequest.builder()
                .transferId(transfer.getId().toString())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .build();

        try {
            ledgerServiceClient.postTransfer(ledgerRequest).block();
            transfer.setStatus(TransferStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Ledger service call failed for transfer {}", transfer.getId(), e);
            transfer.setStatus(TransferStatus.FAILED);
        }

        Transfer completedTransfer = transferRepository.save(transfer);
        TransferView transferView = toTransferView(completedTransfer);

        // Store the idempotency key and response
        try {
            IdempotencyKey newKey = IdempotencyKey.builder()
                    .idempotencyKey(idempotencyKey)
                    .responseBody(objectMapper.writeValueAsString(transferView))
                    .responseStatusCode(200) // Assuming success
                    .expiryAt(LocalDateTime.now().plusHours(24))
                    .build();
            idempotencyKeyRepository.saveAndFlush(newKey);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize response for idempotent storage", e);
        }


        return transferView;
    }

    @Override
    public TransferView getTransfer(UUID id) {
        return transferRepository.findById(id)
                .map(this::toTransferView)
                .orElseThrow(() -> new RuntimeException("Transfer not found")); // Replace with specific exception
    }

    @Override
    public List<CompletableFuture<TransferView>> createBatchTransfers(List<CreateTransferRequest> requests) {
        return requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() ->
                        createTransfer(UUID.randomUUID(), request), batchExecutor))
                .toList();
    }

    private TransferView toTransferView(Transfer transfer) {
        return TransferView.builder()
                .id(transfer.getId())
                .fromAccountId(transfer.getFromAccountId())
                .toAccountId(transfer.getToAccountId())
                .amount(transfer.getAmount())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
