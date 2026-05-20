package com.example.expensechallenge.api.dto;

import com.example.expensechallenge.domain.PurchaseTransaction;
import com.example.expensechallenge.service.FxRate;
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
        String converted = MoneyFormatter.format(
            tx.purchaseAmountUsd().multiply(rate.exchangeRate())
        );

        return new ConvertedTransactionResponse(
            tx.id(),
            tx.description(),
            tx.transactionDate(),
            MoneyFormatter.format(tx.purchaseAmountUsd()),
            currency.toUpperCase(),
            rate.exchangeRate().toPlainString(),
            converted,
            rate.rateDate()
        );
    }
}
