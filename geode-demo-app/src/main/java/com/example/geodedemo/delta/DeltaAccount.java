package com.example.geodedemo.delta;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.Delta;
import org.apache.geode.InvalidDeltaException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Delta-enabled Account class for efficient update propagation.
 *
 * Delta Propagation:
 * - Only sends changed fields instead of entire object
 * - Reduces network bandwidth significantly
 * - Improves replication performance
 *
 * Requirements:
 * - Implement org.apache.geode.Delta interface
 * - Track which fields have changed
 * - Implement toDelta() and fromDelta() methods
 *
 * Best for:
 * - Large objects with frequent partial updates
 * - High-throughput scenarios
 * - WAN replication optimization
 */
@Data
@Slf4j
public class DeltaAccount implements Delta, Serializable {

    private static final long serialVersionUID = 1L;

    // Account fields
    private String accountId;
    private String customerId;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Delta tracking flags
    private transient boolean balanceChanged = false;
    private transient boolean statusChanged = false;
    private transient boolean creditLimitChanged = false;

    public DeltaAccount() {
    }

    public DeltaAccount(String accountId, String customerId, String accountType, BigDecimal balance) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = balance;
        this.status = "ACTIVE";
        this.creditLimit = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Delta-aware setters

    public void setBalance(BigDecimal balance) {
        if (this.balance == null || !this.balance.equals(balance)) {
            this.balance = balance;
            this.balanceChanged = true;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setStatus(String status) {
        if (this.status == null || !this.status.equals(status)) {
            this.status = status;
            this.statusChanged = true;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        if (this.creditLimit == null || !this.creditLimit.equals(creditLimit)) {
            this.creditLimit = creditLimit;
            this.creditLimitChanged = true;
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Delta interface implementation

    @Override
    public boolean hasDelta() {
        boolean hasDelta = balanceChanged || statusChanged || creditLimitChanged;
        log.debug("[Delta] hasDelta() = {} for account {}", hasDelta, accountId);
        return hasDelta;
    }

    @Override
    public void toDelta(DataOutput out) throws IOException {
        log.debug("[Delta] toDelta() - Writing delta for account {}", accountId);

        // Write flags indicating which fields changed
        out.writeBoolean(balanceChanged);
        out.writeBoolean(statusChanged);
        out.writeBoolean(creditLimitChanged);

        // Write only changed fields
        if (balanceChanged) {
            out.writeUTF(balance != null ? balance.toString() : "0");
            log.debug("[Delta] Writing balance: {}", balance);
        }

        if (statusChanged) {
            out.writeUTF(status != null ? status : "");
            log.debug("[Delta] Writing status: {}", status);
        }

        if (creditLimitChanged) {
            out.writeUTF(creditLimit != null ? creditLimit.toString() : "0");
            log.debug("[Delta] Writing creditLimit: {}", creditLimit);
        }

        // Always write updated timestamp
        out.writeUTF(updatedAt != null ? updatedAt.toString() : LocalDateTime.now().toString());

        // Reset flags after sending delta
        resetDeltaFlags();
    }

    @Override
    public void fromDelta(DataInput in) throws IOException, InvalidDeltaException {
        log.debug("[Delta] fromDelta() - Reading delta for account {}", accountId);

        // Read flags
        boolean readBalance = in.readBoolean();
        boolean readStatus = in.readBoolean();
        boolean readCreditLimit = in.readBoolean();

        // Apply only changed fields
        if (readBalance) {
            String balanceStr = in.readUTF();
            this.balance = new BigDecimal(balanceStr);
            log.debug("[Delta] Applied balance: {}", balance);
        }

        if (readStatus) {
            this.status = in.readUTF();
            log.debug("[Delta] Applied status: {}", status);
        }

        if (readCreditLimit) {
            String creditLimitStr = in.readUTF();
            this.creditLimit = new BigDecimal(creditLimitStr);
            log.debug("[Delta] Applied creditLimit: {}", creditLimit);
        }

        // Read updated timestamp
        String updatedAtStr = in.readUTF();
        this.updatedAt = LocalDateTime.parse(updatedAtStr);
    }

    /**
     * Reset delta tracking flags after delta is sent.
     */
    public void resetDeltaFlags() {
        balanceChanged = false;
        statusChanged = false;
        creditLimitChanged = false;
    }

    /**
     * Create a full copy (for initial replication or when delta is not applicable).
     */
    public DeltaAccount copy() {
        DeltaAccount copy = new DeltaAccount();
        copy.accountId = this.accountId;
        copy.customerId = this.customerId;
        copy.accountNumber = this.accountNumber;
        copy.accountType = this.accountType;
        copy.balance = this.balance;
        copy.creditLimit = this.creditLimit;
        copy.status = this.status;
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;
        return copy;
    }
}
