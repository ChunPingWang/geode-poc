package com.example.geodedemo.controller;

import lombok.RequiredArgsConstructor;
import org.apache.geode.cache.GemFireCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final GemFireCache cache;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("application", "geode-demo-app");

        try {
            boolean connected = !cache.isClosed();
            status.put("geode", connected ? "CONNECTED" : "DISCONNECTED");
            status.put("cacheName", cache.getName());
        } catch (Exception e) {
            status.put("geode", "ERROR: " + e.getMessage());
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/regions")
    public ResponseEntity<Map<String, Object>> regions() {
        Map<String, Object> info = new HashMap<>();

        cache.rootRegions().forEach(region -> {
            Map<String, Object> regionInfo = new HashMap<>();
            regionInfo.put("name", region.getName());
            regionInfo.put("size", region.size());
            regionInfo.put("fullPath", region.getFullPath());
            info.put(region.getName(), regionInfo);
        });

        return ResponseEntity.ok(info);
    }
}
