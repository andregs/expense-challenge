package com.example.expensechallenge.api.dto;

import java.util.List;

public record TransactionPageResponse(
    List<TransactionResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
