package com.example.geodedemo.service;

import com.example.geodedemo.entity.Account;
import com.example.geodedemo.exception.ResourceNotFoundException;
import com.example.geodedemo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Account createAccount(Account account) {
        account.setAccountId(UUID.randomUUID().toString());
        account.setAccountNumber(generateAccountNumber());
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        if (account.getCreditLimit() == null) {
            account.setCreditLimit(BigDecimal.ZERO);
        }

        Account saved = accountRepository.save(account);
        log.info("Created account: {} for customer: {}", saved.getAccountId(), saved.getCustomerId());
        return saved;
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    public List<Account> getAccountsByCustomer(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public Account deposit(String accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Deposit amount must be positive");
        }

        Account account = getAccount(accountId);
        account.credit(amount);

        Account saved = accountRepository.save(account);
        log.info("Deposited {} to account: {}. New balance: {}", amount, accountId, saved.getBalance());
        return saved;
    }

    public Account withdraw(String accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Withdrawal amount must be positive");
        }

        Account account = getAccount(accountId);
        account.debit(amount);

        Account saved = accountRepository.save(account);
        log.info("Withdrew {} from account: {}. New balance: {}", amount, accountId, saved.getBalance());
        return saved;
    }

    public Account transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Transfer amount must be positive");
        }

        Account fromAccount = getAccount(fromAccountId);
        Account toAccount = getAccount(toAccountId);

        fromAccount.debit(amount);
        toAccount.credit(amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("Transferred {} from {} to {}", amount, fromAccountId, toAccountId);
        return fromAccount;
    }

    public List<Account> getAllAccounts() {
        return (List<Account>) accountRepository.findAll();
    }

    private String generateAccountNumber() {
        return String.format("%010d", System.nanoTime() % 10000000000L);
    }
}
