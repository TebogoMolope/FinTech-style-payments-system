package com.example.ledgerservice.controller;

import com.example.ledgerservice.dto.AccountView;
import com.example.ledgerservice.dto.CreateAccountRequest;
import com.example.ledgerservice.dto.LedgerTransferRequest;
import com.example.ledgerservice.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Ledger and Account Management API")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new account")
    public AccountView createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ledgerService.createAccount(request);
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get account details by ID")
    public AccountView getAccount(@PathVariable("id") Long id) {
        return ledgerService.getAccount(id);
    }

    @PostMapping("/ledger/transfer")
    @Operation(summary = "Apply a ledger transfer")
    @ApiResponse(responseCode = "200", description = "Transfer applied successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input, e.g., insufficient funds")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "409", description = "Duplicate transfer ID")
    public ResponseEntity<Void> applyTransfer(@Valid @RequestBody LedgerTransferRequest request) {
        ledgerService.applyTransfer(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Ledger service is healthy");
    }
}
