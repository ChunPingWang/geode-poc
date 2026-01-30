package com.example.geodedemo.repository;

import com.example.geodedemo.entity.Account;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.query.annotation.Trace;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends GemfireRepository<Account, String> {

    @Trace
    List<Account> findByCustomerId(String customerId);

    @Trace
    List<Account> findByStatus(Account.AccountStatus status);

    @Trace
    List<Account> findByAccountType(Account.AccountType accountType);
}
