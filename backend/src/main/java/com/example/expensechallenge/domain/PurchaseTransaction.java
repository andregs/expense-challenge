package com.example.expensechallenge.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

@Table("purchase_transactions")
public record PurchaseTransaction(
    @Id UUID id,
    String description,
    LocalDate transactionDate,
    BigDecimal purchaseAmountUsd,
    @ReadOnlyProperty OffsetDateTime createdAt
) {
    public static PurchaseTransaction newPurchase(
        String description, LocalDate transactionDate, BigDecimal purchaseAmountUsd
    ) {
        return new PurchaseTransaction(null, description, transactionDate, purchaseAmountUsd, null);
    }
}
