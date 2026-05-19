package com.example.expensechallenge.infrastructure.treasury;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(TreasuryClient.CIRCUIT_BREAKER_NAME)
public record TreasuryClientConfig(String baseUrl) {
}
