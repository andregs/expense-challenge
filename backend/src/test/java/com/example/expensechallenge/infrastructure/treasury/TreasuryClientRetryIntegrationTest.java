package com.example.expensechallenge.infrastructure.treasury;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.expensechallenge.AbstractWireMockIntegrationTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

/**
 * Verifies that the {@code @Retryable} annotation on
 * {@link TreasuryClient#fetchLatestRate} fires correctly when the Treasury
 * API returns 5xx responses. Uses the full Spring context so Spring Retry's
 * AOP proxy is active (plain instantiation bypasses the proxy).
 *
 * <p>Retry delay is set to 0 ms in {@code application-test.yml} to keep tests fast.
 */
class TreasuryClientRetryIntegrationTest extends AbstractWireMockIntegrationTest {

    private static final String PATH = "/v1/accounting/od/rates_of_exchange";
    private static final LocalDate DATE = LocalDate.of(2026, 4, 15);

    @Autowired
    TreasuryClient treasuryClient;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    @Test
    void succeeds_afterTwoFailuresThenSuccess() {
        // Two 500s followed by a 200 — should succeed on the third attempt.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
            .inScenario("retry-success")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("first-retry"));

        wireMock.stubFor(get(urlPathEqualTo(PATH))
            .inScenario("retry-success")
            .whenScenarioStateIs("first-retry")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("second-retry"));

        wireMock.stubFor(get(urlPathEqualTo(PATH))
            .inScenario("retry-success")
            .whenScenarioStateIs("second-retry")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "data": [{
                        "country_currency_desc": "Brazil-Real",
                        "exchange_rate": "5.1234",
                        "record_date": "2026-03-31"
                      }]
                    }
                    """)));

        Optional<TreasuryRateDto> result = treasuryClient.fetchLatestRate("Brazil-Real", DATE);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().exchangeRate()).isEqualByComparingTo("5.1234");
        // Must have hit the server exactly 3 times (2 failures + 1 success)
        wireMock.verify(3, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    void exhaustsRetries_andPropagatesException() {
        // All attempts return 500 — should throw after max-retries (3) + initial = 4 total attempts.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> treasuryClient.fetchLatestRate("Brazil-Real", DATE))
            .isInstanceOf(RestClientException.class);

        // 1 initial + 3 retries = 4 total requests
        wireMock.verify(4, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    void clientError_isNotRetried() {
        // 4xx should propagate immediately without retry.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> treasuryClient.fetchLatestRate("Brazil-Real", DATE))
            .isInstanceOf(RestClientException.class);

        // No retry — exactly 1 request
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    }
}
