package com.example.expensechallenge.service;

import com.example.expensechallenge.infrastructure.treasury.TreasuryClient;
import com.example.expensechallenge.service.exception.UnconvertibleException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class FxRateService {

    private final TreasuryClient treasuryClient;
    private final Map<String, String> currencyDescriptions;

    public FxRateService(TreasuryClient treasuryClient, JsonMapper jsonMapper) {
        this.treasuryClient = treasuryClient;
        this.currencyDescriptions = loadCurrencies(jsonMapper);
    }

    /**
     * Returns the applicable exchange rate for the given ISO 4217 currency code
     * relative to the purchase date. The rate must have
     * {@code record_date <= purchaseDate} and be no older than 6 months;
     * otherwise {@link UnconvertibleException} is thrown.
     *
     * <p>Lookups are cached in Redis (cache name {@code fxRates}, TTL from
     * {@code fx-cache.ttl}) under a quarter-granularity key. Treasury publishes
     * exchange rates quarterly, so any two purchase dates that fall in the same
     * {year, quarter} resolve to the same most-recent rate; caching at quarter
     * granularity keeps the cache small without sacrificing correctness.
     * Exceptions thrown by this method are not cached, so transient failures
     * (Treasury down, no rate yet published) are retried on the next call.
     */
    @Cacheable(
        cacheNames = "fxRates",
        key = "#currencyCode.toUpperCase() + ':' + #purchaseDate.year + ':' + ((#purchaseDate.monthValue - 1) / 3 + 1)"
    )
    public FxRate getRate(String currencyCode, LocalDate purchaseDate) {
        String currencyDesc = currencyDescriptions.get(currencyCode.toUpperCase());
        if (currencyDesc == null) {
            throw new UnconvertibleException("Unsupported currency: " + currencyCode);
        }

        var dto = treasuryClient.fetchLatestRate(currencyDesc, purchaseDate)
            .orElseThrow(() -> new UnconvertibleException(
                "No Treasury exchange rate available for " + currencyCode
                + " on or before " + purchaseDate));

        if (dto.recordDate().isBefore(purchaseDate.minusMonths(6))) {
            throw new UnconvertibleException(
                "Most recent exchange rate for " + currencyCode
                + " (dated " + dto.recordDate() + ") is older than 6 months"
                + " relative to transaction date " + purchaseDate);
        }

        return new FxRate(dto.exchangeRate(), dto.recordDate());
    }

    /**
     * Loads the ISO 4217 → Treasury {@code country_currency_desc} mapping
     * from {@code currencies.json} on the classpath. The descriptions must
     * match the Treasury dataset exactly — they are used verbatim in the
     * {@code filter=country_currency_desc:eq:<desc>} query parameter, and a
     * typo silently produces "no rate" 422 responses for the affected code.
     *
     * <p>To add a currency: look up its row in the
     * <a href="https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange">
     * Treasury Reporting Rates of Exchange</a> dataset (the "Country-Currency"
     * column is the source of truth) and append it to
     * {@code src/main/resources/currencies.json} using the ISO 4217 code as the
     * JSON key.
     */
    private static Map<String, String> loadCurrencies(JsonMapper jsonMapper) {
        try (InputStream is = new ClassPathResource("currencies.json").getInputStream()) {
            return jsonMapper.readValue(is, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load currencies.json from classpath", e);
        }
    }
}
