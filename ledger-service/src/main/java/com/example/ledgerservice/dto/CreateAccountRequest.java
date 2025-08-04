package com.example.ledgerservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotNull(message = "Initial balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance must be non-negative")
    private BigDecimal initialBalance;
}
