package com.example.expensechallenge.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTransactionRequest(
    @NotBlank @Size(max = 50) String description,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate transactionDate,
    @NotNull
    @Digits(integer = 15, fraction = 2)
    @DecimalMin(value = "0.01", message = "must be greater than zero")
    String purchaseAmountUsd
) {}
