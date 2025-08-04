package com.example.transferservice.client;

import com.example.transferservice.dto.LedgerTransferRequest;
import com.example.transferservice.exception.LedgerServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceClient {

    private final WebClient ledgerWebClient;

    @CircuitBreaker(name = "ledgerService", fallbackMethod = "fallbackPostTransfer")
    public Mono<Void> postTransfer(LedgerTransferRequest transferRequest) {
        return ledgerWebClient.post()
                .uri("/ledger/transfer")
                .bodyValue(transferRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new LedgerServiceException(
                                        "Ledger service failed with status " + response.statusCode() + ": " + errorBody)))
                )
                .bodyToMono(Void.class);
    }

    public Mono<Void> fallbackPostTransfer(LedgerTransferRequest transferRequest, Throwable t) {
        log.error("Ledger service is unavailable. Falling back for transfer {}", transferRequest.getTransferId(), t);
        return Mono.error(new LedgerServiceException("Ledger service is unavailable. Please try again later."));
    }
}
