package com.example.geodedemo.service;

import com.example.geodedemo.entity.Customer;
import com.example.geodedemo.exception.ResourceNotFoundException;
import com.example.geodedemo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer createCustomer(Customer customer) {
        customer.setCustomerId(UUID.randomUUID().toString());
        customer.setStatus(Customer.CustomerStatus.ACTIVE);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        Customer saved = customerRepository.save(customer);
        log.info("Created customer: {}", saved.getCustomerId());
        return saved;
    }

    public Customer getCustomer(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    public Customer updateCustomer(String customerId, Customer updates) {
        Customer customer = getCustomer(customerId);

        if (updates.getName() != null) {
            customer.setName(updates.getName());
        }
        if (updates.getEmail() != null) {
            customer.setEmail(updates.getEmail());
        }
        if (updates.getPhone() != null) {
            customer.setPhone(updates.getPhone());
        }
        if (updates.getAddress() != null) {
            customer.setAddress(updates.getAddress());
        }
        customer.setUpdatedAt(LocalDateTime.now());

        Customer saved = customerRepository.save(customer);
        log.info("Updated customer: {}", customerId);
        return saved;
    }

    public void deleteCustomer(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        customerRepository.deleteById(customerId);
        log.info("Deleted customer: {}", customerId);
    }

    public List<Customer> getAllCustomers() {
        return (List<Customer>) customerRepository.findAll();
    }

    public List<Customer> findByStatus(Customer.CustomerStatus status) {
        return customerRepository.findByStatus(status);
    }

    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email: " + email));
    }
}
