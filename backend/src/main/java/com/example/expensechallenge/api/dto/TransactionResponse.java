package com.example.expensechallenge.api.dto;

import com.example.expensechallenge.domain.PurchaseTransaction;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String description,
    LocalDate transactionDate,
    String purchaseAmountUsd
) {
    public static TransactionResponse from(PurchaseTransaction tx) {
        return new TransactionResponse(
            tx.id(),
            tx.description(),
            tx.transactionDate(),
            MoneyFormatter.format(tx.purchaseAmountUsd())
        );
    }
}
