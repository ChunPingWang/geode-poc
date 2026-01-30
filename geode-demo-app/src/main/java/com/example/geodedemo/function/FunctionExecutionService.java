package com.example.geodedemo.function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing Geode functions on the server-side.
 * Functions run where data resides, reducing network overhead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionExecutionService {

    private final GemFireCache cache;

    @PostConstruct
    public void init() {
        // Register functions
        FunctionService.registerFunction(new AccountAggregationFunction());
        log.info("Registered AccountAggregationFunction");
    }

    /**
     * Execute aggregation function on Accounts region.
     *
     * @param operation Operation type: TOTAL_BALANCE, COUNT_BY_TYPE, AVERAGE_BALANCE, MIN_MAX_BALANCE, SUMMARY
     * @return Aggregation result
     */
    @SuppressWarnings("unchecked")
    public Object executeAccountAggregation(String operation) {
        Region<String, ?> region = cache.getRegion("Accounts");
        if (region == null) {
            log.warn("Accounts region not found");
            return Map.of("error", "Accounts region not found");
        }

        try {
            Execution execution = FunctionService.onRegion(region)
                .setArguments(operation);

            ResultCollector<?, ?> rc = execution.execute(AccountAggregationFunction.ID);
            List<?> results = (List<?>) rc.getResult(30, TimeUnit.SECONDS);

            if (results != null && !results.isEmpty()) {
                // Aggregate results from all servers
                return aggregateResults(results, operation);
            }
            return Map.of("error", "No results returned");

        } catch (Exception e) {
            log.error("Function execution failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Aggregate results from multiple servers.
     */
    private Object aggregateResults(List<?> results, String operation) {
        if (results.size() == 1) {
            return results.get(0);
        }

        // For multiple results (from multiple servers), aggregate based on operation
        switch (operation != null ? operation.toUpperCase() : "TOTAL_BALANCE") {
            case "TOTAL_BALANCE":
            case "AVERAGE_BALANCE":
                // Sum numeric results
                java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                for (Object result : results) {
                    if (result instanceof java.math.BigDecimal) {
                        total = total.add((java.math.BigDecimal) result);
                    }
                }
                return total;

            case "COUNT_BY_TYPE":
                // Merge count maps
                Map<String, Long> mergedCounts = new java.util.HashMap<>();
                for (Object result : results) {
                    if (result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Long> counts = (Map<String, Long>) result;
                        counts.forEach((k, v) -> mergedCounts.merge(k, v, Long::sum));
                    }
                }
                return mergedCounts;

            default:
                return results;
        }
    }

    /**
     * Get total balance across all accounts.
     */
    public Object getTotalBalance() {
        return executeAccountAggregation("TOTAL_BALANCE");
    }

    /**
     * Get account count by type.
     */
    public Object getCountByType() {
        return executeAccountAggregation("COUNT_BY_TYPE");
    }

    /**
     * Get average balance.
     */
    public Object getAverageBalance() {
        return executeAccountAggregation("AVERAGE_BALANCE");
    }

    /**
     * Get comprehensive summary.
     */
    public Object getSummary() {
        return executeAccountAggregation("SUMMARY");
    }
}
