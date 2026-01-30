package com.example.geodedemo.cache;

import com.example.geodedemo.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.CacheWriterException;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.util.CacheWriterAdapter;

/**
 * CacheWriter for Customer region - implements write-through pattern.
 *
 * CacheWriter is called BEFORE the cache operation completes.
 * Use cases:
 * - Synchronous write to external database
 * - Validation before data is stored
 * - Audit logging
 *
 * If CacheWriter throws an exception, the cache operation is aborted.
 */
@Slf4j
public class CustomerCacheWriter extends CacheWriterAdapter<String, Customer> {

    @Override
    public void beforeCreate(EntryEvent<String, Customer> event) throws CacheWriterException {
        Customer customer = event.getNewValue();
        log.info("[CacheWriter] Before CREATE - Key: {}, Customer: {}",
            event.getKey(), customer != null ? customer.getName() : "null");

        // Validation example
        if (customer != null) {
            validateCustomer(customer);
        }

        // Write-through to external database would go here
        // externalDatabase.insert(customer);

        log.debug("[CacheWriter] Write-through completed for key: {}", event.getKey());
    }

    @Override
    public void beforeUpdate(EntryEvent<String, Customer> event) throws CacheWriterException {
        Customer oldValue = event.getOldValue();
        Customer newValue = event.getNewValue();
        log.info("[CacheWriter] Before UPDATE - Key: {}, Old: {}, New: {}",
            event.getKey(),
            oldValue != null ? oldValue.getName() : "null",
            newValue != null ? newValue.getName() : "null");

        if (newValue != null) {
            validateCustomer(newValue);
        }

        // Write-through to external database
        // externalDatabase.update(newValue);
    }

    @Override
    public void beforeDestroy(EntryEvent<String, Customer> event) throws CacheWriterException {
        log.info("[CacheWriter] Before DESTROY - Key: {}", event.getKey());

        // Write-through delete to external database
        // externalDatabase.delete(event.getKey());
    }

    /**
     * Validate customer data before storing.
     * Throws CacheWriterException to abort the operation if validation fails.
     */
    private void validateCustomer(Customer customer) throws CacheWriterException {
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new CacheWriterException("Customer name cannot be empty");
        }

        if (customer.getEmail() != null && !customer.getEmail().contains("@")) {
            throw new CacheWriterException("Invalid email format: " + customer.getEmail());
        }

        // Additional validations can be added here
    }
}
