package com.example.expensechallenge.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensechallenge.TestcontainersConfiguration;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full-stack GET /api/v1/transactions/{id} integration tests.
 *
 * <p>WireMock is started in a static initialiser so it is running before
 * {@link DynamicPropertySource} wires the Treasury base URL into the Spring
 * context. Each test clears both the WireMock stubs and the Redis FX cache
 * so tests are fully isolated despite sharing the same application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TransactionGetIntegrationTest {

    private static final WireMockServer wireMock = startWireMock();

    private static WireMockServer startWireMock() {
        var server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        return server;
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("treasury.base-url", wireMock::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired MockMvc mockMvc;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        Cache cache = cacheManager.getCache("fxRates");
        if (cache != null) {
            cache.clear();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID seedTransaction(String date, String amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Test","transactionDate":"%s","purchaseAmountUsd":"%s"}
                    """.formatted(date, amount)))
            .andExpect(status().isCreated())
            .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private void stubTreasuryRate(String countryCurrencyDesc, String exchangeRate, String rateDate) {
        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"data":[{"country_currency_desc":"%s","exchange_rate":"%s","record_date":"%s"}],"meta":{"count":1}}
                    """.formatted(countryCurrencyDesc, exchangeRate, rateDate))));
    }

    private void stubTreasuryEmpty() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"data":[],"meta":{"count":0}}
                    """)));
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void happyPath_returnsConvertedTransaction() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasuryRate("Brazil-Real", "5.1234", "2026-03-31");

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.description").value("Test"))
            .andExpect(jsonPath("$.transactionDate").value("2026-04-15"))
            .andExpect(jsonPath("$.purchaseAmountUsd").value("100.00"))
            .andExpect(jsonPath("$.currency").value("BRL"))
            .andExpect(jsonPath("$.exchangeRate").value("5.1234"))
            .andExpect(jsonPath("$.convertedAmount").value("512.34"))
            .andExpect(jsonPath("$.rateDate").value("2026-03-31"));
    }

    @Test
    void halfUpRounding_convertedAmountIsCorrectlyRounded() throws Exception {
        // 1.00 × 1.2355 = 1.2355 → HALF_UP to 2dp → 1.24
        UUID id = seedTransaction("2026-04-15", "1.00");
        stubTreasuryRate("Brazil-Real", "1.2355", "2026-03-31");

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.convertedAmount").value("1.24"));
    }

    @Test
    void noCurrencyParam_returnsPlainTransaction() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");

        mockMvc.perform(get("/api/v1/transactions/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.purchaseAmountUsd").value("100.00"))
            .andExpect(jsonPath("$.currency").doesNotExist())
            .andExpect(jsonPath("$.convertedAmount").doesNotExist());
    }

    @Test
    void cacheHit_treasuryCalledOnlyOnce() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasuryRate("Brazil-Real", "5.1234", "2026-03-31");

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.convertedAmount").value("512.34"));

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v1/accounting/od/rates_of_exchange")));
    }

    @Test
    void unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID()).param("currency", "BRL"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void noTreasuryRate_returns422() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasuryEmpty();

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void rateTooOld_returns422() throws Exception {
        // purchaseDate = 2026-04-15; boundary = 2025-10-15; one day beyond = 2025-10-14
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasuryRate("Brazil-Real", "5.0", "2025-10-14");

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void unsupportedCurrency_returns422() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "XYZ"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
