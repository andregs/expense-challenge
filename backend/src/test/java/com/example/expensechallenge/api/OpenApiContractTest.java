package com.example.expensechallenge.api;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.expensechallenge.AbstractWireMockIntegrationTest;
import com.example.expensechallenge.service.FxRateService;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;

/**
 * Validates that every controller response conforms to the OpenAPI contract
 * defined in {@code packages/api-contract/openapi.yaml}.
 *
 * <p>
 * The spec is loaded from the test classpath (the api-contract directory is
 * added as a test resource srcDir in {@code build.gradle.kts}). Each test
 * exercises one endpoint variant and asserts only schema conformance — the
 * business-logic assertions live in the dedicated integration tests.
 *
 * <p>
 * WireMock stubs the Treasury upstream for the conversion test so the full
 * GET path (including FX lookup) is exercised without hitting the real API.
 */
class OpenApiContractTest extends AbstractWireMockIntegrationTest {

    private static final String SPEC = "openapi.yaml";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        Cache cache = cacheManager.getCache(FxRateService.CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID seedTransaction(String date, String amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"description":"Laptop","transactionDate":"%s","purchaseAmountUsd":"%s"}
                        """.formatted(date, amount)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private void stubTreasury(String countryCurrencyDesc, String rate, String rateDate) {
        wireMock.stubFor(WireMock.get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                        {"data":[{"country_currency_desc":"%s","exchange_rate":"%s","record_date":"%s"}],"meta":{"count":1}}
                                        """
                                        .formatted(countryCurrencyDesc, rate, rateDate))));
    }

    private void stubTreasuryEmpty() {
        wireMock.stubFor(WireMock.get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"data":[],"meta":{"count":0}}
                                """)));
    }

    // ── POST /api/v1/transactions ─────────────────────────────────────────────

    @Test
    void createTransaction_201_conformsToSpec() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"description":"Office supplies","transactionDate":"2026-04-15","purchaseAmountUsd":"49.99"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(SPEC));
    }

    // ── GET /api/v1/transactions ──────────────────────────────────────────────

    @Test
    void listTransactions_200_conformsToSpec() throws Exception {
        seedTransaction("2026-04-15", "100.00");

        mockMvc.perform(get("/api/v1/transactions").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    // ── GET /api/v1/transactions/{id} ─────────────────────────────────────────

    @Test
    void getTransaction_200_noCurrency_conformsToSpec() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");

        mockMvc.perform(get("/api/v1/transactions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void getTransaction_200_withConversion_conformsToSpec() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasury("Brazil-Real", "5.1234", "2026-03-31");

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void getTransaction_404_conformsToSpec() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void getTransaction_422_noRate_conformsToSpec() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasuryEmpty();

        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(openApi().isValid(SPEC));
    }
}
