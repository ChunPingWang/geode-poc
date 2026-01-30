package com.example.geodedemo.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom metrics for Geode operations.
 */
@Slf4j
@Service
@EnableScheduling
public class GeodeMetricsService {

    private final MeterRegistry meterRegistry;
    private final GemFireCache cache;

    // Counters
    private final AtomicLong customerCount = new AtomicLong(0);
    private final AtomicLong accountCount = new AtomicLong(0);
    private final AtomicLong cqEventCount = new AtomicLong(0);
    private final AtomicLong transactionCount = new AtomicLong(0);
    private final AtomicLong failedTransactionCount = new AtomicLong(0);

    // Timers
    private Timer readTimer;
    private Timer writeTimer;
    private Timer transactionTimer;

    public GeodeMetricsService(MeterRegistry meterRegistry, GemFireCache cache) {
        this.meterRegistry = meterRegistry;
        this.cache = cache;
    }

    @PostConstruct
    public void init() {
        // Register region size gauges
        Gauge.builder("geode.region.size", customerCount, AtomicLong::get)
            .tag("region", "Customers")
            .description("Number of entries in Customers region")
            .register(meterRegistry);

        Gauge.builder("geode.region.size", accountCount, AtomicLong::get)
            .tag("region", "Accounts")
            .description("Number of entries in Accounts region")
            .register(meterRegistry);

        // CQ event counter
        Gauge.builder("geode.cq.events.total", cqEventCount, AtomicLong::get)
            .description("Total CQ events received")
            .register(meterRegistry);

        // Transaction counters
        Gauge.builder("geode.transactions.total", transactionCount, AtomicLong::get)
            .description("Total transactions executed")
            .register(meterRegistry);

        Gauge.builder("geode.transactions.failed", failedTransactionCount, AtomicLong::get)
            .description("Failed transactions")
            .register(meterRegistry);

        // Timers
        readTimer = Timer.builder("geode.operation.duration")
            .tag("operation", "read")
            .description("Time taken for read operations")
            .register(meterRegistry);

        writeTimer = Timer.builder("geode.operation.duration")
            .tag("operation", "write")
            .description("Time taken for write operations")
            .register(meterRegistry);

        transactionTimer = Timer.builder("geode.transaction.duration")
            .description("Time taken for transactions")
            .register(meterRegistry);

        // Cache status gauge
        Gauge.builder("geode.cache.connected", () -> cache.isClosed() ? 0 : 1)
            .description("Whether cache is connected (1) or closed (0)")
            .register(meterRegistry);

        log.info("Geode metrics initialized");
    }

    @Scheduled(fixedRate = 30000) // Update every 30 seconds
    public void updateRegionMetrics() {
        try {
            Region<?, ?> customersRegion = cache.getRegion("Customers");
            if (customersRegion != null) {
                customerCount.set(customersRegion.size());
            }

            Region<?, ?> accountsRegion = cache.getRegion("Accounts");
            if (accountsRegion != null) {
                accountCount.set(accountsRegion.size());
            }
        } catch (Exception e) {
            log.debug("Error updating region metrics: {}", e.getMessage());
        }
    }

    // Methods to record metrics from other services

    public void recordRead(long durationNanos) {
        readTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordWrite(long durationNanos) {
        writeTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordTransaction(long durationNanos, boolean success) {
        transactionTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        transactionCount.incrementAndGet();
        if (!success) {
            failedTransactionCount.incrementAndGet();
        }
    }

    public void recordCqEvent() {
        cqEventCount.incrementAndGet();
    }
}
