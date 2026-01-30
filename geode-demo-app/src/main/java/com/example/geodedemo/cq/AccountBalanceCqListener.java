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
        // Note: CqEvent doesn't provide oldValue directly in Geode
        // We track previous balance in the event store if needed

        String eventType = mapOperationType(operation);
        log.debug("CQ Event: {} for account {}", eventType,
            newValue != null ? newValue.getAccountId() : "unknown");

        if (newValue == null) {
            log.debug("Skipping event with null value");
            return;
        }

        BigDecimal newBalance = newValue.getBalance();
        // Get previous balance from event store if available
        BigDecimal oldBalance = eventStore.getLastKnownBalance(newValue.getAccountId());
        BigDecimal changeAmount = oldBalance != null ?
            newBalance.subtract(oldBalance) : BigDecimal.ZERO;

        // Determine alert type
        String alertType = determineAlertType(newBalance, changeAmount);

        BalanceChangeEvent event = BalanceChangeEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .accountId(newValue.getAccountId())
            .customerId(newValue.getCustomerId())
            .eventType(eventType)
            .oldBalance(oldBalance != null ? oldBalance : BigDecimal.ZERO)
            .newBalance(newBalance)
            .changeAmount(changeAmount)
            .timestamp(LocalDateTime.now())
            .alertType(alertType)
            .build();

        eventStore.addEvent(event);
        eventStore.updateLastKnownBalance(newValue.getAccountId(), newBalance);

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
