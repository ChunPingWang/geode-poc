package com.example.geodedemo.controller;

import com.example.geodedemo.cq.BalanceChangeEvent;
import com.example.geodedemo.cq.ContinuousQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Continuous Query management and event monitoring.
 */
@RestController
@RequestMapping("/api/cq")
@RequiredArgsConstructor
public class ContinuousQueryController {

    private final ContinuousQueryService cqService;

    /**
     * Get list of active CQs.
     */
    @GetMapping("/queries")
    public ResponseEntity<Map<String, Object>> getActiveQueries() {
        return ResponseEntity.ok(Map.of(
            "activeQueries", cqService.getActiveQueries(),
            "count", cqService.getActiveQueries().size()
        ));
    }

    /**
     * Register the default account balance CQ.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDefaultCq() {
        try {
            cqService.registerAccountBalanceCq();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Account balance CQ registered"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Register a custom CQ with OQL query.
     */
    @PostMapping("/register/custom")
    public ResponseEntity<Map<String, Object>> registerCustomCq(@RequestBody CustomCqRequest request) {
        try {
            cqService.registerCustomCq(request.getName(), request.getQuery());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Custom CQ '" + request.getName() + "' registered"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Stop a specific CQ.
     */
    @DeleteMapping("/queries/{name}")
    public ResponseEntity<Map<String, Object>> stopQuery(@PathVariable String name) {
        cqService.stopQuery(name);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "CQ '" + name + "' stopped"
        ));
    }

    /**
     * Get recent events.
     */
    @GetMapping("/events")
    public ResponseEntity<List<BalanceChangeEvent>> getRecentEvents(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(cqService.getRecentEvents(limit));
    }

    /**
     * Get alerts only.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<BalanceChangeEvent>> getAlerts() {
        return ResponseEntity.ok(cqService.getAlerts());
    }

    /**
     * Get current thresholds.
     */
    @GetMapping("/thresholds")
    public ResponseEntity<Map<String, BigDecimal>> getThresholds() {
        return ResponseEntity.ok(cqService.getThresholds());
    }

    /**
     * Update alert thresholds.
     */
    @PutMapping("/thresholds")
    public ResponseEntity<Map<String, Object>> updateThresholds(@RequestBody ThresholdRequest request) {
        cqService.updateThresholds(request.getLowBalance(), request.getLargeTransaction());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "thresholds", cqService.getThresholds()
        ));
    }

    @lombok.Data
    public static class CustomCqRequest {
        private String name;
        private String query;
    }

    @lombok.Data
    public static class ThresholdRequest {
        private BigDecimal lowBalance;
        private BigDecimal largeTransaction;
    }
}
