package com.example.geodedemo.async;

import com.example.geodedemo.entity.Account;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Async Event Listener for Account operations.
 *
 * AsyncEventListener provides asynchronous event processing:
 * - Events are queued and processed in batch
 * - Non-blocking - doesn't slow down cache operations
 * - Persistent queue option for reliability
 *
 * Use cases:
 * - Asynchronous replication to external systems
 * - Event-driven architecture integration
 * - Analytics and reporting pipelines
 * - Write-behind to slow external systems
 */
@Slf4j
public class AccountAsyncEventListener implements AsyncEventListener {

    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong batchCount = new AtomicLong(0);

    @Override
    public boolean processEvents(List<AsyncEvent> events) {
        batchCount.incrementAndGet();
        log.info("[AsyncEventListener] Processing batch of {} events", events.size());

        for (AsyncEvent event : events) {
            try {
                processEvent(event);
                processedCount.incrementAndGet();
            } catch (Exception e) {
                log.error("[AsyncEventListener] Error processing event: {}", e.getMessage());
                // Return false to retry the entire batch
                // In production, implement more sophisticated error handling
                return false;
            }
        }

        log.info("[AsyncEventListener] Batch processed successfully. Total processed: {}",
            processedCount.get());
        return true;
    }

    private void processEvent(AsyncEvent event) {
        String key = (String) event.getKey();
        Account account = (Account) event.getDeserializedValue();
        String operation = event.getOperation().name();

        log.debug("[AsyncEventListener] Event - Op: {}, Key: {}, Account: {}",
            operation, key, account != null ? account.getAccountId() : "null");

        // Process based on operation type
        switch (event.getOperation()) {
            case CREATE:
                handleCreate(key, account);
                break;
            case UPDATE:
                handleUpdate(key, account);
                break;
            case DESTROY:
                handleDestroy(key);
                break;
            default:
                log.debug("[AsyncEventListener] Unhandled operation: {}", operation);
        }
    }

    private void handleCreate(String key, Account account) {
        // Send to external analytics system
        log.info("[AsyncEventListener] New account created: {} with balance {}",
            key, account != null ? account.getBalance() : "unknown");

        // In production:
        // analyticsService.trackAccountCreation(account);
        // messagingService.publishAccountCreatedEvent(account);
    }

    private void handleUpdate(String key, Account account) {
        // Track account changes
        log.info("[AsyncEventListener] Account updated: {} new balance {}",
            key, account != null ? account.getBalance() : "unknown");

        // In production:
        // analyticsService.trackAccountUpdate(account);
        // Check for fraud patterns, etc.
    }

    private void handleDestroy(String key) {
        log.info("[AsyncEventListener] Account deleted: {}", key);

        // In production:
        // analyticsService.trackAccountDeletion(key);
    }

    @Override
    public void close() {
        log.info("[AsyncEventListener] Closed. Total events processed: {}, Batches: {}",
            processedCount.get(), batchCount.get());
    }

    public long getProcessedCount() { return processedCount.get(); }
    public long getBatchCount() { return batchCount.get(); }
}
