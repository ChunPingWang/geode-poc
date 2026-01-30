package com.example.geodedemo.cache;

import com.example.geodedemo.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.util.CacheListenerAdapter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * CacheListener for Customer region - monitors cache events.
 *
 * CacheListener is called AFTER the cache operation completes.
 * Use cases:
 * - Event logging and auditing
 * - Metrics collection
 * - Triggering downstream processes
 * - Notifications
 *
 * Unlike CacheWriter, CacheListener cannot abort the operation.
 */
@Slf4j
public class CustomerCacheListener extends CacheListenerAdapter<String, Customer> {

    private final AtomicLong createCount = new AtomicLong(0);
    private final AtomicLong updateCount = new AtomicLong(0);
    private final AtomicLong deleteCount = new AtomicLong(0);

    @Override
    public void afterCreate(EntryEvent<String, Customer> event) {
        createCount.incrementAndGet();
        Customer customer = event.getNewValue();
        log.info("[CacheListener] CREATED - Key: {}, Customer: {}, Region: {}",
            event.getKey(),
            customer != null ? customer.getName() : "null",
            event.getRegion().getName());

        // Trigger downstream processes
        // messagingService.sendCustomerCreatedEvent(customer);
    }

    @Override
    public void afterUpdate(EntryEvent<String, Customer> event) {
        updateCount.incrementAndGet();
        Customer oldValue = event.getOldValue();
        Customer newValue = event.getNewValue();
        log.info("[CacheListener] UPDATED - Key: {}, Old: {}, New: {}",
            event.getKey(),
            oldValue != null ? oldValue.getName() : "null",
            newValue != null ? newValue.getName() : "null");

        // Check for significant changes
        if (oldValue != null && newValue != null) {
            if (!oldValue.getStatus().equals(newValue.getStatus())) {
                log.warn("[CacheListener] Status changed for customer {}: {} -> {}",
                    event.getKey(), oldValue.getStatus(), newValue.getStatus());
            }
        }
    }

    @Override
    public void afterDestroy(EntryEvent<String, Customer> event) {
        deleteCount.incrementAndGet();
        log.info("[CacheListener] DELETED - Key: {}, Region: {}",
            event.getKey(), event.getRegion().getName());

        // Cleanup related data
        // relatedDataService.cleanupForCustomer(event.getKey());
    }

    @Override
    public void afterInvalidate(EntryEvent<String, Customer> event) {
        log.info("[CacheListener] INVALIDATED - Key: {}", event.getKey());
    }

    /**
     * Get event statistics.
     */
    public String getStats() {
        return String.format("Creates: %d, Updates: %d, Deletes: %d",
            createCount.get(), updateCount.get(), deleteCount.get());
    }

    public long getCreateCount() { return createCount.get(); }
    public long getUpdateCount() { return updateCount.get(); }
    public long getDeleteCount() { return deleteCount.get(); }
}
