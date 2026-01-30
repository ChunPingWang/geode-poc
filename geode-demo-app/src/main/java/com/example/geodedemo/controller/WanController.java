package com.example.geodedemo.controller;

import com.example.geodedemo.wan.WanReplicationInfo;
import com.example.geodedemo.wan.WanReplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for WAN replication information and testing.
 */
@Slf4j
@RestController
@RequestMapping("/api/wan")
@RequiredArgsConstructor
public class WanController {

    private final WanReplicationService wanReplicationService;

    /**
     * Get WAN replication configuration info.
     * GET /api/wan/info
     */
    @GetMapping("/info")
    public ResponseEntity<WanReplicationInfo> getWanInfo() {
        log.info("Getting WAN replication info");
        return ResponseEntity.ok(wanReplicationService.getWanInfo());
    }

    /**
     * Get pool connection info.
     * GET /api/wan/pools
     */
    @GetMapping("/pools")
    public ResponseEntity<List<WanReplicationService.PoolInfo>> getPoolInfo() {
        log.info("Getting pool info");
        return ResponseEntity.ok(wanReplicationService.getPoolInfo());
    }

    /**
     * Get WAN-enabled regions.
     * GET /api/wan/regions
     */
    @GetMapping("/regions")
    public ResponseEntity<List<String>> getWanEnabledRegions() {
        log.info("Getting WAN-enabled regions");
        return ResponseEntity.ok(wanReplicationService.getWanEnabledRegions());
    }

    /**
     * Test WAN replication by writing test data.
     * POST /api/wan/test
     *
     * Request body:
     * {
     *   "testKey": "wan-test-001",
     *   "testValue": "test data from site A"
     * }
     */
    @PostMapping("/test")
    public ResponseEntity<WanReplicationService.WanTestResult> testWanReplication(
            @RequestBody WanTestRequest request) {
        log.info("Testing WAN replication with key: {}", request.getTestKey());
        return ResponseEntity.ok(
            wanReplicationService.testWanReplication(request.getTestKey(), request.getTestValue())
        );
    }

    /**
     * Get comprehensive WAN status summary.
     * GET /api/wan/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWanStatus() {
        log.info("Getting comprehensive WAN status");

        Map<String, Object> status = new HashMap<>();
        status.put("wanInfo", wanReplicationService.getWanInfo());
        status.put("pools", wanReplicationService.getPoolInfo());
        status.put("regions", wanReplicationService.getWanEnabledRegions());
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }

    @lombok.Data
    public static class WanTestRequest {
        private String testKey;
        private String testValue;
    }
}
