package com.example.geodedemo.cq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event representing a balance change detected by Continuous Query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChangeEvent {
    private String eventId;
    private String accountId;
    private String customerId;
    private String eventType;  // CREATED, UPDATED, DESTROYED
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private BigDecimal changeAmount;
    private LocalDateTime timestamp;
    private String alertType;  // LOW_BALANCE, HIGH_BALANCE, LARGE_TRANSACTION
}
