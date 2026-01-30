package com.example.geodedemo.controller;

import com.example.geodedemo.service.TransactionService;
import com.example.geodedemo.service.TransactionService.TransferResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for ACID transaction operations.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Transfer funds with ACID transaction guarantees.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResult> transfer(@RequestBody TransferRequest request) {
        TransferResult result = transactionService.transferWithTransaction(
            request.getFromAccountId(),
            request.getToAccountId(),
            request.getAmount()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Batch update multiple accounts in a single transaction.
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdate(@RequestBody Map<String, BigDecimal> adjustments) {
        boolean success = transactionService.batchUpdateWithTransaction(adjustments);

        return ResponseEntity.ok(Map.of(
            "success", success,
            "accountsUpdated", adjustments.size()
        ));
    }

    @lombok.Data
    public static class TransferRequest {
        private String fromAccountId;
        private String toAccountId;
        private BigDecimal amount;
    }
}
