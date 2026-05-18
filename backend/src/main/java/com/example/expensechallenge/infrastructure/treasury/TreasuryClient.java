package com.example.expensechallenge.infrastructure.treasury;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TreasuryClient {

    private final RestClient restClient;

    public TreasuryClient(RestClient.Builder builder, TreasuryClientConfig config) {
        this.restClient = builder.baseUrl(config.baseUrl()).build();
    }

    /**
     * Fetches the most recent Treasury rate for the given currency with
     * {@code record_date <= onOrBefore}. Returns {@code Optional.empty()} when
     * Treasury reports no matching row. Network and protocol errors propagate
     * as {@code RestClientException} subclasses so callers can decide on
     * retry/cache-fallback strategies. Connection and read timeouts are
     * configured globally via {@code spring.http.client.*} so every outbound
     * HTTP call shares the same SLA.
     */
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
