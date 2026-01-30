package com.example.geodedemo.cache;

import com.example.geodedemo.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * CacheLoader for Customer region - implements read-through pattern.
 *
 * CacheLoader is called when a cache miss occurs (key not found in cache).
 * Use cases:
 * - Load data from external database on cache miss
 * - Lazy loading of data
 * - Transparent data population
 *
 * The loaded value is automatically stored in the cache.
 */
@Slf4j
public class CustomerCacheLoader implements CacheLoader<String, Customer> {

    // Simulated external database (in production, connect to actual database)
    private static final Map<String, Customer> EXTERNAL_DATABASE = new HashMap<>();

    static {
        // Pre-populate some sample data in "external database"
        Customer sample1 = new Customer();
        sample1.setCustomerId("EXT-001");
        sample1.setName("External User 1");
        sample1.setEmail("external1@example.com");
        sample1.setStatus(Customer.CustomerStatus.ACTIVE);
        sample1.setCreatedAt(LocalDateTime.now());
        sample1.setUpdatedAt(LocalDateTime.now());
        EXTERNAL_DATABASE.put("EXT-001", sample1);

        Customer sample2 = new Customer();
        sample2.setCustomerId("EXT-002");
        sample2.setName("External User 2");
        sample2.setEmail("external2@example.com");
        sample2.setStatus(Customer.CustomerStatus.ACTIVE);
        sample2.setCreatedAt(LocalDateTime.now());
        sample2.setUpdatedAt(LocalDateTime.now());
        EXTERNAL_DATABASE.put("EXT-002", sample2);
    }

    @Override
    public Customer load(LoaderHelper<String, Customer> helper) throws CacheLoaderException {
        String key = helper.getKey();
        log.info("[CacheLoader] Cache miss for key: {} - Loading from external source", key);

        // Simulate loading from external database
        Customer customer = loadFromExternalDatabase(key);

        if (customer != null) {
            log.info("[CacheLoader] Loaded customer from external source: {}", customer.getName());
        } else {
            log.debug("[CacheLoader] Customer not found in external source: {}", key);
        }

        return customer;
    }

    /**
     * Load customer from external database.
     * In production, this would query an actual database.
     */
    private Customer loadFromExternalDatabase(String key) {
        // Simulate database query delay
        try {
            Thread.sleep(10); // 10ms simulated latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check simulated external database
        Customer customer = EXTERNAL_DATABASE.get(key);

        if (customer != null) {
            // Create a copy to avoid reference issues
            Customer loaded = new Customer();
            loaded.setCustomerId(customer.getCustomerId());
            loaded.setName(customer.getName());
            loaded.setEmail(customer.getEmail());
            loaded.setPhone(customer.getPhone());
            loaded.setAddress(customer.getAddress());
            loaded.setStatus(customer.getStatus());
            loaded.setCreatedAt(customer.getCreatedAt());
            loaded.setUpdatedAt(LocalDateTime.now()); // Update timestamp on load
            return loaded;
        }

        return null;
    }

    /**
     * Add data to simulated external database (for testing).
     */
    public static void addToExternalDatabase(String key, Customer customer) {
        EXTERNAL_DATABASE.put(key, customer);
    }

    /**
     * Clear simulated external database (for testing).
     */
    public static void clearExternalDatabase() {
        EXTERNAL_DATABASE.clear();
    }

    @Override
    public void close() {
        log.info("[CacheLoader] Closed");
    }
}
