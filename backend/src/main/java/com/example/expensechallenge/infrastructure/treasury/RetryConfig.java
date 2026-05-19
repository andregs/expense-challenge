package com.example.expensechallenge.infrastructure.treasury;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Enables Spring Framework 7's {@code @Retryable} support.
 *
 * <p>The circuit breaker for the Treasury FiscalData API is configured via
 * {@code resilience4j.circuitbreaker.instances.treasury} in {@code application.yml}.
 * The two mechanisms compose: {@code @Retryable} on {@link TreasuryClient} retries
 * transient failures (network errors, 5xx) with exponential backoff. The circuit
 * breaker, applied in {@code FxRateService}, opens after 50 % of calls fail —
 * skipping retries entirely when Treasury is clearly unavailable.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
class RetryConfig {}
