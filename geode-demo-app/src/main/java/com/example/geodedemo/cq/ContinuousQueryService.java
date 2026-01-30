package com.example.geodedemo.cq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.query.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Continuous Queries on Geode regions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContinuousQueryService {

    private final GemFireCache cache;
    private final EventStore eventStore;

    private final Map<String, CqQuery> activeQueries = new ConcurrentHashMap<>();

    // Configurable thresholds
    private BigDecimal lowBalanceThreshold = new BigDecimal("100");
    private BigDecimal largeTransactionThreshold = new BigDecimal("1000");

    @PostConstruct
    public void init() {
        try {
            // Register default CQ for all account changes
            registerAccountBalanceCq();
            log.info("Continuous Query service initialized");
        } catch (Exception e) {
            log.warn("Could not initialize CQ on startup: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        stopAllQueries();
    }

    /**
     * Register CQ to monitor all account balance changes.
     */
    public void registerAccountBalanceCq() throws CqException, CqExistsException, RegionNotFoundException {
        String queryName = "AccountBalanceMonitor";

        if (activeQueries.containsKey(queryName)) {
            log.info("CQ {} already registered", queryName);
            return;
        }

        String queryString = "SELECT * FROM /Accounts";

        ClientCache clientCache = (ClientCache) cache;
        QueryService queryService = clientCache.getQueryService();

        CqAttributesFactory cqf = new CqAttributesFactory();
        cqf.addCqListener(new AccountBalanceCqListener(
            eventStore,
            lowBalanceThreshold,
            largeTransactionThreshold
        ));

        CqQuery cq = queryService.newCq(queryName, queryString, cqf.create());
        cq.execute();

        activeQueries.put(queryName, cq);
        log.info("CQ registered: {} - {}", queryName, queryString);
    }

    /**
     * Register a custom CQ with specific criteria.
     */
    public void registerCustomCq(String name, String oqlQuery) throws CqException, CqExistsException, RegionNotFoundException {
        if (activeQueries.containsKey(name)) {
            throw new IllegalStateException("CQ with name '" + name + "' already exists");
        }

        ClientCache clientCache = (ClientCache) cache;
        QueryService queryService = clientCache.getQueryService();

        CqAttributesFactory cqf = new CqAttributesFactory();
        cqf.addCqListener(new AccountBalanceCqListener(
            eventStore,
            lowBalanceThreshold,
            largeTransactionThreshold
        ));

        CqQuery cq = queryService.newCq(name, oqlQuery, cqf.create());
        cq.execute();

        activeQueries.put(name, cq);
        log.info("Custom CQ registered: {} - {}", name, oqlQuery);
    }

    /**
     * Stop a specific CQ.
     */
    public void stopQuery(String name) {
        CqQuery cq = activeQueries.remove(name);
        if (cq != null) {
            try {
                cq.stop();
                cq.close();
                log.info("CQ stopped: {}", name);
            } catch (Exception e) {
                log.error("Error stopping CQ {}: {}", name, e.getMessage());
            }
        }
    }

    /**
     * Stop all active CQs.
     */
    public void stopAllQueries() {
        activeQueries.forEach((name, cq) -> {
            try {
                cq.stop();
                cq.close();
                log.info("CQ stopped: {}", name);
            } catch (Exception e) {
                log.error("Error stopping CQ {}: {}", name, e.getMessage());
            }
        });
        activeQueries.clear();
    }

    /**
     * Get list of active CQ names.
     */
    public List<String> getActiveQueries() {
        return List.copyOf(activeQueries.keySet());
    }

    /**
     * Get recent events from the event store.
     */
    public List<BalanceChangeEvent> getRecentEvents(int limit) {
        return eventStore.getRecentEvents(limit);
    }

    /**
     * Get alerts (events with alert type set).
     */
    public List<BalanceChangeEvent> getAlerts() {
        return eventStore.getAlerts();
    }

    /**
     * Update thresholds for alerts.
     */
    public void updateThresholds(BigDecimal lowBalance, BigDecimal largeTransaction) {
        this.lowBalanceThreshold = lowBalance;
        this.largeTransactionThreshold = largeTransaction;
        log.info("Thresholds updated - Low balance: {}, Large transaction: {}",
            lowBalance, largeTransaction);
    }

    public Map<String, BigDecimal> getThresholds() {
        return Map.of(
            "lowBalanceThreshold", lowBalanceThreshold,
            "largeTransactionThreshold", largeTransactionThreshold
        );
    }
}
