package com.example.transferservice.controller;

import com.example.transferservice.dto.CreateTransferRequest;
import com.example.transferservice.dto.TransferView;
import com.example.transferservice.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Transfer Management API")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Initiate a new transfer")
    public TransferView createTransfer(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request) {
        return transferService.createTransfer(idempotencyKey, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer details by ID")
    public TransferView getTransfer(@PathVariable UUID id) {
        return transferService.getTransfer(id);
    }

    @PostMapping("/batch")
    @Operation(summary = "Initiate a batch of transfers")
    public List<TransferView> createBatchTransfers(@Valid @RequestBody List<CreateTransferRequest> requests) {
        if (requests.size() > 20) {
            throw new IllegalArgumentException("Batch size cannot exceed 20 transfers.");
        }
        return transferService.createBatchTransfers(requests).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Error processing batch transfer", e);
                    }
                })
                .toList();
    }
}
