package com.example.ledgerservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransferRequest {

    @NotEmpty(message = "Transfer ID cannot be empty")
    private String transferId;

    @NotNull(message = "From account ID cannot be null")
    private Long fromAccountId;

    @NotNull(message = "To account ID cannot be null")
    private Long toAccountId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Transfer amount must be positive")
    private BigDecimal amount;
}
