package com.example.geodedemo.repository;

import com.example.geodedemo.entity.Customer;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.query.annotation.Trace;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends GemfireRepository<Customer, String> {

    @Trace
    Optional<Customer> findByEmail(String email);

    @Trace
    List<Customer> findByStatus(Customer.CustomerStatus status);

    @Trace
    List<Customer> findByNameContaining(String name);
}
