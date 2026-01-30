package com.example.geodedemo.controller;

import com.example.geodedemo.function.FunctionExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Geode function execution.
 * Functions execute on the server-side for better performance.
 */
@Slf4j
@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionExecutionService functionService;

    /**
     * Execute account aggregation function.
     * GET /api/functions/accounts/aggregate?operation=TOTAL_BALANCE
     *
     * Operations: TOTAL_BALANCE, COUNT_BY_TYPE, AVERAGE_BALANCE, MIN_MAX_BALANCE, SUMMARY
     */
    @GetMapping("/accounts/aggregate")
    public ResponseEntity<Object> executeAggregation(
            @RequestParam(defaultValue = "SUMMARY") String operation) {
        log.info("Executing aggregation function: {}", operation);
        Object result = functionService.executeAccountAggregation(operation);
        return ResponseEntity.ok(Map.of(
            "operation", operation,
            "result", result
        ));
    }

    /**
     * Get total balance across all accounts.
     * GET /api/functions/accounts/total-balance
     */
    @GetMapping("/accounts/total-balance")
    public ResponseEntity<Object> getTotalBalance() {
        log.info("Getting total balance");
        return ResponseEntity.ok(Map.of(
            "operation", "TOTAL_BALANCE",
            "totalBalance", functionService.getTotalBalance()
        ));
    }

    /**
     * Get account count by type.
     * GET /api/functions/accounts/count-by-type
     */
    @GetMapping("/accounts/count-by-type")
    public ResponseEntity<Object> getCountByType() {
        log.info("Getting count by type");
        return ResponseEntity.ok(Map.of(
            "operation", "COUNT_BY_TYPE",
            "counts", functionService.getCountByType()
        ));
    }

    /**
     * Get average balance.
     * GET /api/functions/accounts/average-balance
     */
    @GetMapping("/accounts/average-balance")
    public ResponseEntity<Object> getAverageBalance() {
        log.info("Getting average balance");
        return ResponseEntity.ok(Map.of(
            "operation", "AVERAGE_BALANCE",
            "averageBalance", functionService.getAverageBalance()
        ));
    }

    /**
     * Get comprehensive account summary.
     * GET /api/functions/accounts/summary
     */
    @GetMapping("/accounts/summary")
    public ResponseEntity<Object> getSummary() {
        log.info("Getting account summary");
        return ResponseEntity.ok(Map.of(
            "operation", "SUMMARY",
            "summary", functionService.getSummary()
        ));
    }
}
