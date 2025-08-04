package com.example.transferservice.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    @Value("${ledger.service.base-url}")
    private String ledgerServiceBaseUrl;

    @Bean
    public WebClient ledgerWebClient() {
        return WebClient.builder()
                .baseUrl(ledgerServiceBaseUrl)
                .filter(correlationIdPropagationFilter())
                .build();
    }

    private ExchangeFilterFunction correlationIdPropagationFilter() {
        return (clientRequest, next) -> {
            String correlationId = MDC.get(CORRELATION_ID_LOG_VAR_NAME);
            ClientRequest newRequest = ClientRequest.from(clientRequest)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .build();
            return next.exchange(newRequest);
        };
    }
}
