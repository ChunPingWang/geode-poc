package com.example.geodedemo.cq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory store for CQ events.
 * In production, this would be replaced with Kafka, Redis, or a database.
 */
@Slf4j
@Component
public class EventStore {

    private final ConcurrentLinkedDeque<BalanceChangeEvent> events = new ConcurrentLinkedDeque<>();
    private final Map<String, BigDecimal> lastKnownBalances = new ConcurrentHashMap<>();
    private static final int MAX_EVENTS = 1000;

    public void addEvent(BalanceChangeEvent event) {
        events.addFirst(event);
        log.info("Event stored: {} - Account {} balance changed to {}",
            event.getEventType(), event.getAccountId(), event.getNewBalance());

        // Keep only the most recent events
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    public List<BalanceChangeEvent> getRecentEvents(int limit) {
        List<BalanceChangeEvent> result = new ArrayList<>();
        int count = 0;
        for (BalanceChangeEvent event : events) {
            if (count >= limit) break;
            result.add(event);
            count++;
        }
        return result;
    }

    public List<BalanceChangeEvent> getEventsByAccount(String accountId) {
        return events.stream()
            .filter(e -> e.getAccountId().equals(accountId))
            .toList();
    }

    public List<BalanceChangeEvent> getAlerts() {
        return events.stream()
            .filter(e -> e.getAlertType() != null)
            .toList();
    }

    public void clear() {
        events.clear();
        lastKnownBalances.clear();
    }

    public int size() {
        return events.size();
    }

    public BigDecimal getLastKnownBalance(String accountId) {
        return lastKnownBalances.get(accountId);
    }

    public void updateLastKnownBalance(String accountId, BigDecimal balance) {
        lastKnownBalances.put(accountId, balance);
    }
}
