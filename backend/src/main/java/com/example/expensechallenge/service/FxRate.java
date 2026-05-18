package com.example.expensechallenge.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FxRate(BigDecimal exchangeRate, LocalDate rateDate) {}
