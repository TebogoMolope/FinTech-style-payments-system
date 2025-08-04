package com.example.transferservice.dto;

import com.example.transferservice.domain.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferView {
    private UUID id;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private TransferStatus status;
    private LocalDateTime createdAt;
}
