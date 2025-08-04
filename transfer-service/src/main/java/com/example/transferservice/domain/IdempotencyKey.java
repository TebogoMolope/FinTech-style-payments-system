package com.example.transferservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    private UUID idempotencyKey;

    @Column(nullable = false)
    private String responseBody;

    @Column(nullable = false)
    private int responseStatusCode;

    @Column(nullable = false)
    private LocalDateTime expiryAt;
}
