package com.example.expensechallenge.infrastructure.treasury;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpServerErrorException;

@Component
public class TreasuryClient {

    public static final String CIRCUIT_BREAKER_NAME = "treasury";

    private final RestClient restClient;

    public TreasuryClient(RestClient.Builder builder, TreasuryClientConfig config) {
        this.restClient = builder.baseUrl(config.baseUrl()).build();
    }

    /**
     * Fetches the most recent Treasury rate for the given currency with
     * {@code record_date <= onOrBefore}. Returns {@code Optional.empty()} when
     * Treasury reports no matching row.
     *
     * <p>Transient failures (network errors and 5xx responses) are retried up
     * to 3 times with exponential backoff starting at 300 ms. Client errors
     * (4xx) are not retried and propagate immediately. After retry exhaustion
     * the last exception propagates to the caller.
     *
     * @see <a href="https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange">
     *      Treasury Reporting Rates of Exchange — dataset docs</a>
     * @see <a href="https://fiscaldata.treasury.gov/api-documentation/">
     *      FiscalData API documentation</a>
     */
    @Retryable(
        includes = {ResourceAccessException.class, HttpServerErrorException.class},
        maxRetriesString = "${treasury.retry.max-retries:3}",
        delayString = "${treasury.retry.delay:300}",
        multiplierString = "${treasury.retry.multiplier:2.0}"
    )
    public Optional<TreasuryRateDto> fetchLatestRate(String currencyDesc, LocalDate onOrBefore) {
        String filter = "country_currency_desc:eq:" + currencyDesc
            + ",record_date:lte:" + onOrBefore;

        TreasuryResponse response = restClient.get()
            .uri(b -> b
                .path("/v1/accounting/od/rates_of_exchange")
                .queryParam("fields", "country_currency_desc,exchange_rate,record_date")
                .queryParam("filter", filter)
                .queryParam("sort", "-record_date")
                .queryParam("page[size]", 1)
                .build())
            .retrieve()
            .body(TreasuryResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(response.data().getFirst());
    }

    record TreasuryResponse(List<TreasuryRateDto> data) {}
}
