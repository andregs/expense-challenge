package com.example.expensechallenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

import com.example.expensechallenge.infrastructure.treasury.TreasuryClient;
import com.example.expensechallenge.infrastructure.treasury.TreasuryRateDto;
import com.example.expensechallenge.service.exception.UnconvertibleException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Unit-level test for {@link FxRateService}. {@code @Cacheable} is a no-op
 * here because the service is instantiated directly (no Spring proxy), so
 * every call exercises the underlying logic. Redis cache behaviour lands in
 * {@code TransactionGetIntegrationTest} (Step 19), which spins up a real
 * Redis container.
 *
 * <p>
 * The circuit breaker is stubbed as a pass-through so tests drive
 * {@link TreasuryClient} behaviour directly without open-state interference.
 */
@ExtendWith(MockitoExtension.class)
class FxRateServiceTest {

    @Mock
    private TreasuryClient treasuryClient;
    @Mock
    private Resilience4JCircuitBreakerFactory circuitBreakerFactory;
    @Mock
    private CircuitBreaker circuitBreaker;

    private FxRateService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(circuitBreakerFactory.create(TreasuryClient.CIRCUIT_BREAKER_NAME))
                .thenReturn(circuitBreaker);
        org.mockito.Mockito.lenient().when(circuitBreaker.run(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
                    java.util.function.Supplier<?> supplier = inv.getArgument(0);
                    return supplier.get();
                });
        service = new FxRateService(treasuryClient, circuitBreakerFactory, JsonMapper.builder().build());
    }

    @Test
    void happyPath_returnsRateFromTreasury() {
        LocalDate purchaseDate = LocalDate.of(2026, 4, 15);
        LocalDate rateDate = LocalDate.of(2026, 3, 31);
        when(treasuryClient.fetchLatestRate("Brazil-Real", purchaseDate))
                .thenReturn(Optional.of(new TreasuryRateDto("Brazil-Real", new BigDecimal("5.1234"), rateDate)));

        FxRate result = service.getRate("BRL", purchaseDate);

        assertThat(result.exchangeRate()).isEqualByComparingTo("5.1234");
        assertThat(result.rateDate()).isEqualTo(rateDate);
    }

    @Test
    void rateDateExactly6MonthsBefore_passes() {
        LocalDate purchaseDate = LocalDate.of(2026, 4, 15);
        LocalDate rateDate = purchaseDate.minusMonths(6); // boundary inclusive
        when(treasuryClient.fetchLatestRate("Brazil-Real", purchaseDate))
                .thenReturn(Optional.of(new TreasuryRateDto("Brazil-Real", new BigDecimal("5.0"), rateDate)));

        FxRate result = service.getRate("BRL", purchaseDate);

        assertThat(result.rateDate()).isEqualTo(rateDate);
    }

    @Test
    void rateDateOneDayBeyond6Months_throwsUnconvertible() {
        LocalDate purchaseDate = LocalDate.of(2026, 4, 15);
        LocalDate rateDate = purchaseDate.minusMonths(6).minusDays(1);
        when(treasuryClient.fetchLatestRate("Brazil-Real", purchaseDate))
                .thenReturn(Optional.of(new TreasuryRateDto("Brazil-Real", new BigDecimal("5.0"), rateDate)));

        assertThatThrownBy(() -> service.getRate("BRL", purchaseDate))
                .isInstanceOf(UnconvertibleException.class)
                .hasMessageContaining("older than 6 months");
    }

    @Test
    void noTreasuryRate_throwsUnconvertible() {
        LocalDate purchaseDate = LocalDate.of(2026, 4, 15);
        when(treasuryClient.fetchLatestRate("Brazil-Real", purchaseDate))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate("BRL", purchaseDate))
                .isInstanceOf(UnconvertibleException.class)
                .hasMessageContaining("No Treasury exchange rate");
    }

    @Test
    void unsupportedCurrencyCode_throwsUnconvertible_treasuryNotCalled() {
        assertThatThrownBy(() -> service.getRate("XYZ", LocalDate.of(2026, 4, 15)))
                .isInstanceOf(UnconvertibleException.class)
                .hasMessageContaining("Unsupported currency");
        verify(treasuryClient, never()).fetchLatestRate(any(), any());
    }

    @Test
    void currencyCodeIsCaseInsensitive() {
        LocalDate purchaseDate = LocalDate.of(2026, 4, 15);
        LocalDate rateDate = LocalDate.of(2026, 3, 31);
        when(treasuryClient.fetchLatestRate("Brazil-Real", purchaseDate))
                .thenReturn(Optional.of(new TreasuryRateDto("Brazil-Real", new BigDecimal("5.1234"), rateDate)));

        FxRate result = service.getRate("brl", purchaseDate);

        assertThat(result.exchangeRate()).isEqualByComparingTo("5.1234");
    }
}
