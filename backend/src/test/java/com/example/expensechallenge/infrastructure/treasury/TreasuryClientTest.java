package com.example.expensechallenge.infrastructure.treasury;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Unit-level test for {@link TreasuryClient}. Instantiates the client directly
 * with a fresh {@link RestClient.Builder} pointed at a per-class WireMock
 * server, so the test starts in milliseconds without booting the Spring
 * context. Coverage for the full Spring-managed wiring lands in the
 * {@code TransactionGetIntegrationTest} added in Step 19.
 */
class TreasuryClientTest {

    private static WireMockServer wireMock;
    private TreasuryClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        var config = new TreasuryClientConfig(wireMock.baseUrl());
        client = new TreasuryClient(RestClient.builder(), config);
    }

    @Test
    void happyPath_parsesCurrencyRateAndDate() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "data": [{
                        "country_currency_desc": "Brazil-Real",
                        "exchange_rate": "5.1234",
                        "record_date": "2026-03-31"
                      }],
                      "meta": {"count": 1}
                    }
                    """)));

        Optional<TreasuryRateDto> result = client.fetchLatestRate(
            "Brazil-Real", LocalDate.of(2026, 4, 15));

        assertThat(result).isPresent();
        TreasuryRateDto dto = result.orElseThrow();
        assertThat(dto.countryCurrencyDesc()).isEqualTo("Brazil-Real");
        assertThat(dto.exchangeRate()).isEqualByComparingTo(new BigDecimal("5.1234"));
        assertThat(dto.recordDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void emptyData_returnsEmptyOptional() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"data": [], "meta": {"count": 0}}
                    """)));

        Optional<TreasuryRateDto> result = client.fetchLatestRate(
            "Atlantis-Doubloon", LocalDate.of(2026, 4, 15));

        assertThat(result).isEmpty();
    }

    @Test
    void serverError_propagatesAsRestClientException() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
            .willReturn(aResponse().withStatus(503).withBody("upstream down")));

        assertThatThrownBy(() -> client.fetchLatestRate(
            "Brazil-Real", LocalDate.of(2026, 4, 15)))
            .isInstanceOf(RestClientException.class);
    }

    @Test
    void issuesExpectedQueryParameters() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"data": [], "meta": {"count": 0}}
                    """)));

        client.fetchLatestRate("Brazil-Real", LocalDate.of(2026, 4, 15));

        List<LoggedRequest> requests = wireMock.findAll(
            getRequestedFor(urlPathEqualTo("/v1/accounting/od/rates_of_exchange")));
        assertThat(requests).hasSize(1);
        LoggedRequest request = requests.getFirst();

        // Query parameters: fields, filter, sort, page[size]. WireMock decodes for us.
        assertThat(request.queryParameter("fields").firstValue())
            .isEqualTo("country_currency_desc,exchange_rate,record_date");
        assertThat(request.queryParameter("filter").firstValue())
            .isEqualTo("country_currency_desc:eq:Brazil-Real,record_date:lte:2026-04-15");
        assertThat(request.queryParameter("sort").firstValue()).isEqualTo("-record_date");
        assertThat(request.queryParameter("page[size]").firstValue()).isEqualTo("1");
    }

}
