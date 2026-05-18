package com.example.expensechallenge.api;

import com.example.expensechallenge.api.dto.CreateTransactionRequest;
import com.example.expensechallenge.api.dto.TransactionPageResponse;
import com.example.expensechallenge.api.dto.TransactionResponse;
import com.example.expensechallenge.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
        @RequestBody @Valid CreateTransactionRequest request,
        UriComponentsBuilder uriBuilder
    ) {
        TransactionResponse response = service.create(request);
        URI location = uriBuilder
            .path("/api/v1/transactions/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public TransactionPageResponse listTransactions(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public Object getTransaction(
        @PathVariable UUID id,
        @RequestParam(required = false) String currency
    ) {
        if (currency == null) {
            return service.get(id);
        }
        return service.getWithConversion(id, currency);
    }
}
