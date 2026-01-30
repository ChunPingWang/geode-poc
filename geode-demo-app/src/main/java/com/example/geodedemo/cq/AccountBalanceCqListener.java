package com.example.geodedemo.cq;

import com.example.geodedemo.entity.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Continuous Query Listener for Account balance changes.
 * Monitors account updates and generates events for significant changes.
 */
@Slf4j
@RequiredArgsConstructor
public class AccountBalanceCqListener implements CqListener {

    private final EventStore eventStore;
    private final BigDecimal lowBalanceThreshold;
    private final BigDecimal largeTransactionThreshold;

    @Override
    public void onEvent(CqEvent cqEvent) {
        Operation operation = cqEvent.getQueryOperation();
        Account newValue = (Account) cqEvent.getNewValue();
        Account oldValue = (Account) cqEvent.getOldValue();

        String eventType = mapOperationType(operation);
        log.debug("CQ Event: {} for account {}", eventType,
            newValue != null ? newValue.getAccountId() : "unknown");

        BigDecimal oldBalance = oldValue != null ? oldValue.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = newValue != null ? newValue.getBalance() : BigDecimal.ZERO;
        BigDecimal changeAmount = newBalance.subtract(oldBalance);

        // Determine alert type
        String alertType = determineAlertType(newBalance, changeAmount);

        BalanceChangeEvent event = BalanceChangeEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .accountId(newValue != null ? newValue.getAccountId() :
                       oldValue != null ? oldValue.getAccountId() : "unknown")
            .customerId(newValue != null ? newValue.getCustomerId() :
                        oldValue != null ? oldValue.getCustomerId() : "unknown")
            .eventType(eventType)
            .oldBalance(oldBalance)
            .newBalance(newBalance)
            .changeAmount(changeAmount)
            .timestamp(LocalDateTime.now())
            .alertType(alertType)
            .build();

        eventStore.addEvent(event);

        if (alertType != null) {
            log.warn("ALERT [{}]: Account {} - Balance: {}, Change: {}",
                alertType, event.getAccountId(), newBalance, changeAmount);
        }
    }

    @Override
    public void onError(CqEvent cqEvent) {
        log.error("CQ Error: {}", cqEvent.getThrowable().getMessage());
    }

    @Override
    public void close() {
        log.info("CQ Listener closed");
    }

    private String mapOperationType(Operation op) {
        if (op.isCreate()) return "CREATED";
        if (op.isUpdate()) return "UPDATED";
        if (op.isDestroy()) return "DESTROYED";
        if (op.isInvalidate()) return "INVALIDATED";
        return "UNKNOWN";
    }

    private String determineAlertType(BigDecimal balance, BigDecimal changeAmount) {
        if (balance.compareTo(lowBalanceThreshold) < 0) {
            return "LOW_BALANCE";
        }
        if (changeAmount.abs().compareTo(largeTransactionThreshold) > 0) {
            return "LARGE_TRANSACTION";
        }
        return null;
    }
}
