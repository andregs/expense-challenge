package com.example.expensechallenge.infrastructure.treasury;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("treasury")
public record TreasuryClientConfig(String baseUrl) {}
