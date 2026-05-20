package com.example.expensechallenge.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import com.example.expensechallenge.AbstractWireMockIntegrationTest;
import com.example.expensechallenge.infrastructure.treasury.TreasuryClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Verifies that the Resilience4j circuit breaker for the Treasury API opens
 * after enough consecutive failures. Uses the same full-context setup as other
 * integration tests (Testcontainers for Postgres/Redis/Kafka) so the real
 * application context is exercised.
 *
 * <p>
 * Each test resets the circuit breaker and flushes the FX cache so that
 * every {@link FxRateService#getRate} call reaches the circuit, regardless
 * of shared context ordering.
 */
class FxCircuitBreakerIntegrationTest extends AbstractWireMockIntegrationTest {

    @Autowired
    FxRateService fxRateService;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        circuitBreakerRegistry.circuitBreaker(TreasuryClient.CIRCUIT_BREAKER_NAME).reset();
        var cache = cacheManager.getCache(FxRateService.CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void circuitOpensAfterMinimumConsecutiveFailures() {
        // Treasury returns 5xx on every request
        wireMock.stubFor(get(urlPathMatching(".*"))
                .willReturn(aResponse().withStatus(500)));

        // minimumNumberOfCalls = 5 — drive 5 failures through the circuit
        LocalDate purchaseDate = LocalDate.of(2026, 4, 15);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> fxRateService.getRate("BRL", purchaseDate))
                    .isInstanceOf(Exception.class);
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(TreasuryClient.CIRCUIT_BREAKER_NAME);
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
