package com.example.expensechallenge.service;

import com.example.expensechallenge.api.dto.ConvertedTransactionResponse;
import com.example.expensechallenge.api.dto.CreateTransactionRequest;
import com.example.expensechallenge.api.dto.TransactionPageResponse;
import com.example.expensechallenge.api.dto.TransactionResponse;
import com.example.expensechallenge.domain.PurchaseTransaction;
import com.example.expensechallenge.infrastructure.persistence.PurchaseTransactionRepository;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final PurchaseTransactionRepository transactionRepository;
    private final OutboxWriter outboxWriter;
    private final FxRateService fxRateService;

    public TransactionService(
        PurchaseTransactionRepository transactionRepository,
        OutboxWriter outboxWriter,
        FxRateService fxRateService
    ) {
        this.transactionRepository = transactionRepository;
        this.outboxWriter = outboxWriter;
        this.fxRateService = fxRateService;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        var tx = PurchaseTransaction.newPurchase(
            request.description(),
            request.transactionDate(),
            new BigDecimal(request.purchaseAmountUsd())
        );
        var saved = transactionRepository.save(tx);
        outboxWriter.write(saved);
        return TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        return transactionRepository.findById(id)
            .map(TransactionResponse::from)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public ConvertedTransactionResponse getWithConversion(UUID id, String currency) {
        PurchaseTransaction tx = transactionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
        FxRate rate = fxRateService.getRate(currency, tx.transactionDate());
        return ConvertedTransactionResponse.from(tx, currency, rate);
    }

    public void evictFxRateCache(UUID id) {
        PurchaseTransaction tx = transactionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
        fxRateService.evictAllRatesForQuarter(tx.transactionDate());
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse list(int page, int size) {
        var items = transactionRepository.findPage(size, page * size).stream()
            .map(TransactionResponse::from)
            .toList();
        long totalElements = transactionRepository.count();
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
        return new TransactionPageResponse(items, page, size, totalElements, totalPages);
    }
}
