package com.example.geodedemo.service;

import com.example.geodedemo.entity.Account;
import com.example.geodedemo.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling ACID transactions in Geode.
 * Demonstrates distributed transaction support with rollback capability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final GemFireCache cache;
    private final Region<String, Account> accountRegion;

    /**
     * Transfer funds between accounts with ACID guarantees.
     * Uses Geode's CacheTransactionManager for distributed transactions.
     */
    public TransferResult transferWithTransaction(String fromAccountId, String toAccountId, BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        CacheTransactionManager txManager = cache.getCacheTransactionManager();

        log.info("Starting transaction {} - Transfer {} from {} to {}",
            transactionId, amount, fromAccountId, toAccountId);

        try {
            // Begin transaction
            txManager.begin();

            // Get accounts within transaction
            Account fromAccount = accountRegion.get(fromAccountId);
            Account toAccount = accountRegion.get(toAccountId);

            if (fromAccount == null) {
                txManager.rollback();
                throw new ResourceNotFoundException("Account", fromAccountId);
            }
            if (toAccount == null) {
                txManager.rollback();
                throw new ResourceNotFoundException("Account", toAccountId);
            }

            // Validate balance
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                txManager.rollback();
                return TransferResult.builder()
                    .transactionId(transactionId)
                    .success(false)
                    .message("Insufficient balance")
                    .fromBalance(fromAccount.getBalance())
                    .toBalance(toAccount.getBalance())
                    .build();
            }

            // Perform transfer
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            fromAccount.setUpdatedAt(LocalDateTime.now());

            toAccount.setBalance(toAccount.getBalance().add(amount));
            toAccount.setUpdatedAt(LocalDateTime.now());

            // Update both accounts
            accountRegion.put(fromAccountId, fromAccount);
            accountRegion.put(toAccountId, toAccount);

            // Commit transaction
            txManager.commit();

            log.info("Transaction {} committed successfully", transactionId);

            return TransferResult.builder()
                .transactionId(transactionId)
                .success(true)
                .message("Transfer completed successfully")
                .fromBalance(fromAccount.getBalance())
                .toBalance(toAccount.getBalance())
                .amount(amount)
                .build();

        } catch (CommitConflictException e) {
            log.error("Transaction {} failed due to conflict: {}", transactionId, e.getMessage());
            if (txManager.exists()) {
                txManager.rollback();
            }
            return TransferResult.builder()
                .transactionId(transactionId)
                .success(false)
                .message("Transaction conflict - please retry")
                .build();

        } catch (Exception e) {
            log.error("Transaction {} failed: {}", transactionId, e.getMessage());
            if (txManager.exists()) {
                txManager.rollback();
            }
            throw e;
        }
    }

    /**
     * Batch update multiple accounts in a single transaction.
     */
    public boolean batchUpdateWithTransaction(java.util.Map<String, BigDecimal> adjustments) {
        CacheTransactionManager txManager = cache.getCacheTransactionManager();
        String transactionId = UUID.randomUUID().toString();

        log.info("Starting batch transaction {} with {} accounts", transactionId, adjustments.size());

        try {
            txManager.begin();

            for (var entry : adjustments.entrySet()) {
                String accountId = entry.getKey();
                BigDecimal adjustment = entry.getValue();

                Account account = accountRegion.get(accountId);
                if (account == null) {
                    txManager.rollback();
                    throw new ResourceNotFoundException("Account", accountId);
                }

                BigDecimal newBalance = account.getBalance().add(adjustment);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    txManager.rollback();
                    log.warn("Batch transaction {} rolled back - insufficient balance for account {}",
                        transactionId, accountId);
                    return false;
                }

                account.setBalance(newBalance);
                account.setUpdatedAt(LocalDateTime.now());
                accountRegion.put(accountId, account);
            }

            txManager.commit();
            log.info("Batch transaction {} committed successfully", transactionId);
            return true;

        } catch (Exception e) {
            log.error("Batch transaction {} failed: {}", transactionId, e.getMessage());
            if (txManager.exists()) {
                txManager.rollback();
            }
            throw e;
        }
    }

    /**
     * Result object for transfer operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransferResult {
        private String transactionId;
        private boolean success;
        private String message;
        private BigDecimal fromBalance;
        private BigDecimal toBalance;
        private BigDecimal amount;
    }
}
