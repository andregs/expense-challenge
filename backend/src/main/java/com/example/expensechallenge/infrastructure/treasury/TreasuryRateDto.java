package com.example.expensechallenge.infrastructure.treasury;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TreasuryRateDto(
    @JsonProperty("country_currency_desc") String countryCurrencyDesc,
    @JsonProperty("exchange_rate") BigDecimal exchangeRate,
    @JsonProperty("record_date") LocalDate recordDate
) {}
