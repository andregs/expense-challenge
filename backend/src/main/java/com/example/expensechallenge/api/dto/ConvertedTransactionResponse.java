package com.example.expensechallenge.api.dto;

import com.example.expensechallenge.domain.PurchaseTransaction;
import com.example.expensechallenge.service.FxRate;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record ConvertedTransactionResponse(
    UUID id,
    String description,
    LocalDate transactionDate,
    String purchaseAmountUsd,
    String currency,
    String exchangeRate,
    String convertedAmount,
    LocalDate rateDate
) {
    public static ConvertedTransactionResponse from(PurchaseTransaction tx, String currency, FxRate rate) {
        String converted = tx.purchaseAmountUsd()
            .multiply(rate.exchangeRate())
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString();

        return new ConvertedTransactionResponse(
            tx.id(),
            tx.description(),
            tx.transactionDate(),
            tx.purchaseAmountUsd().setScale(2, RoundingMode.HALF_UP).toPlainString(),
            currency.toUpperCase(),
            rate.exchangeRate().toPlainString(),
            converted,
            rate.rateDate()
        );
    }
}
