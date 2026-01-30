package com.example.geodedemo.function;

import com.example.geodedemo.entity.Account;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.execute.ResultSender;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side function for account aggregation operations.
 * Executes on the server where data resides to minimize network traffic.
 *
 * Supported operations:
 * - TOTAL_BALANCE: Sum of all account balances
 * - COUNT_BY_TYPE: Count accounts by type
 * - AVERAGE_BALANCE: Average balance across all accounts
 */
@Slf4j
public class AccountAggregationFunction implements Function<String> {

    public static final String ID = "AccountAggregationFunction";

    @Override
    public void execute(FunctionContext<String> context) {
        ResultSender<Object> resultSender = context.getResultSender();
        String operation = context.getArguments();

        try {
            if (context instanceof RegionFunctionContext) {
                RegionFunctionContext rfc = (RegionFunctionContext) context;
                Region<String, Account> region = rfc.getDataSet();

                Object result = executeOperation(region, operation);
                resultSender.lastResult(result);
            } else {
                resultSender.lastResult("Error: Must be executed on a region");
            }
        } catch (Exception e) {
            log.error("Function execution error: {}", e.getMessage());
            resultSender.lastResult("Error: " + e.getMessage());
        }
    }

    private Object executeOperation(Region<String, Account> region, String operation) {
        switch (operation != null ? operation.toUpperCase() : "TOTAL_BALANCE") {
            case "TOTAL_BALANCE":
                return calculateTotalBalance(region);

            case "COUNT_BY_TYPE":
                return countByType(region);

            case "AVERAGE_BALANCE":
                return calculateAverageBalance(region);

            case "MIN_MAX_BALANCE":
                return findMinMaxBalance(region);

            case "SUMMARY":
                return generateSummary(region);

            default:
                return "Unknown operation: " + operation;
        }
    }

    private BigDecimal calculateTotalBalance(Region<String, Account> region) {
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : region.values()) {
            if (account != null && account.getBalance() != null) {
                total = total.add(account.getBalance());
            }
        }
        log.info("Total balance calculated: {}", total);
        return total;
    }

    private Map<String, Long> countByType(Region<String, Account> region) {
        Map<String, Long> counts = new HashMap<>();
        for (Account account : region.values()) {
            if (account != null && account.getAccountType() != null) {
                String type = account.getAccountType();
                counts.merge(type, 1L, Long::sum);
            }
        }
        log.info("Count by type: {}", counts);
        return counts;
    }

    private BigDecimal calculateAverageBalance(Region<String, Account> region) {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (Account account : region.values()) {
            if (account != null && account.getBalance() != null) {
                total = total.add(account.getBalance());
                count++;
            }
        }
        if (count == 0) return BigDecimal.ZERO;
        BigDecimal average = total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
        log.info("Average balance: {}", average);
        return average;
    }

    private Map<String, BigDecimal> findMinMaxBalance(Region<String, Account> region) {
        BigDecimal min = null;
        BigDecimal max = null;
        for (Account account : region.values()) {
            if (account != null && account.getBalance() != null) {
                BigDecimal balance = account.getBalance();
                if (min == null || balance.compareTo(min) < 0) min = balance;
                if (max == null || balance.compareTo(max) > 0) max = balance;
            }
        }
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("min", min != null ? min : BigDecimal.ZERO);
        result.put("max", max != null ? max : BigDecimal.ZERO);
        log.info("Min/Max balance: {}", result);
        return result;
    }

    private Map<String, Object> generateSummary(Region<String, Account> region) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBalance", calculateTotalBalance(region));
        summary.put("averageBalance", calculateAverageBalance(region));
        summary.put("countByType", countByType(region));
        summary.put("minMaxBalance", findMinMaxBalance(region));
        summary.put("totalAccounts", region.size());
        return summary;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public boolean isHA() {
        return true;
    }

    @Override
    public boolean optimizeForWrite() {
        return false;
    }
}
