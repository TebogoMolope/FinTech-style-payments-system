package com.example.transferservice.service;

import com.example.transferservice.dto.CreateTransferRequest;
import com.example.transferservice.dto.TransferView;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TransferService {
    TransferView createTransfer(UUID idempotencyKey, CreateTransferRequest request);
    TransferView getTransfer(UUID id);
    List<CompletableFuture<TransferView>> createBatchTransfers(List<CreateTransferRequest> requests);
}
