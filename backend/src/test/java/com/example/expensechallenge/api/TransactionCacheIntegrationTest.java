package com.example.expensechallenge.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensechallenge.TestcontainersConfiguration;
import com.example.expensechallenge.service.FxRateService;
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
 * Integration tests for DELETE /api/v1/transactions/{id}/cache.
 *
 * <p>The key scenario: after a conversion warms the Redis cache, hitting the
 * eviction endpoint forces the next conversion to call Treasury again.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TransactionCacheIntegrationTest {

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

    @Test
    // Positive complement of TransactionGetIntegrationTest#cacheHit_treasuryCalledOnlyOnce
    void evictCache_forcesSecondConversionToCallTreasury() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");
        stubTreasuryRate("Brazil-Real", "5.1234", "2026-03-31");

        // First GET warms the cache — one Treasury call expected.
        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
                .andExpect(status().isOk());
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v1/accounting/od/rates_of_exchange")));

        // Evict the cache for this transaction's quarter.
        mockMvc.perform(delete("/api/v1/transactions/{id}/cache", id))
                .andExpect(status().isNoContent());

        // Second GET must call Treasury again (cache was cleared).
        mockMvc.perform(get("/api/v1/transactions/{id}", id).param("currency", "BRL"))
                .andExpect(status().isOk());
        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/v1/accounting/od/rates_of_exchange")));
    }

    @Test
    void evictCache_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/transactions/{id}/cache", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void evictCache_idempotent_noErrorWhenNothingCached() throws Exception {
        UUID id = seedTransaction("2026-04-15", "100.00");

        // No conversion has been performed, so nothing is cached — should still return 204.
        mockMvc.perform(delete("/api/v1/transactions/{id}/cache", id))
                .andExpect(status().isNoContent());
    }
}
