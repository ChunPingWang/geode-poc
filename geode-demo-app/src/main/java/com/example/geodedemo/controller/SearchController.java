package com.example.geodedemo.controller;

import com.example.geodedemo.entity.Customer;
import com.example.geodedemo.search.LuceneSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Lucene full-text search operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final LuceneSearchService searchService;

    /**
     * Search customers by name.
     * GET /api/search/customers/name?q=John*
     *
     * Supports wildcards: * (multiple chars), ? (single char)
     */
    @GetMapping("/customers/name")
    public ResponseEntity<Map<String, Object>> searchByName(@RequestParam("q") String query) {
        log.info("Searching customers by name: {}", query);
        List<Customer> results = searchService.searchByName(query);
        return ResponseEntity.ok(Map.of(
            "query", query,
            "field", "name",
            "count", results.size(),
            "results", results
        ));
    }

    /**
     * Search customers by email.
     * GET /api/search/customers/email?q=*@example.com
     */
    @GetMapping("/customers/email")
    public ResponseEntity<Map<String, Object>> searchByEmail(@RequestParam("q") String query) {
        log.info("Searching customers by email: {}", query);
        List<Customer> results = searchService.searchByEmail(query);
        return ResponseEntity.ok(Map.of(
            "query", query,
            "field", "email",
            "count", results.size(),
            "results", results
        ));
    }

    /**
     * Search customers across all indexed fields.
     * GET /api/search/customers?q=john
     */
    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> searchAll(@RequestParam("q") String query) {
        log.info("Searching customers (all fields): {}", query);
        List<Customer> results = searchService.searchAll(query);
        return ResponseEntity.ok(Map.of(
            "query", query,
            "field", "all",
            "count", results.size(),
            "results", results
        ));
    }

    /**
     * Get Lucene index information.
     * GET /api/search/indexes
     */
    @GetMapping("/indexes")
    public ResponseEntity<Map<String, Object>> getIndexInfo() {
        log.info("Getting Lucene index info");
        return ResponseEntity.ok(searchService.getIndexInfo());
    }
}
