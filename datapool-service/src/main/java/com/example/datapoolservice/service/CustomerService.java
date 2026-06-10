package com.example.datapoolservice.service;

import com.example.datapoolservice.model.Customer;
import com.example.datapoolservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createCustomer(Customer customer) {
        Optional<Customer> existing = customerRepository.findByEmail(customer.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Customer with email " + customer.getEmail() + " already exists.");
        }
        return customerRepository.save(customer);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional
    public Optional<Customer> updateCustomer(Long id, Customer customerDetails) {
        Optional<Customer> existing = customerRepository.findByEmail(customerDetails.getEmail());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Customer with email " + customerDetails.getEmail() + " already exists.");
        }
        return customerRepository.findById(id).map(customer -> {
            customer.setName(customerDetails.getName());
            customer.setEmail(customerDetails.getEmail());
            customer.setPhone(customerDetails.getPhone());
            return customerRepository.save(customer);
        });
    }

    @Transactional
    public boolean deleteCustomer(Long id) {
        return customerRepository.findById(id).map(customer -> {
            customerRepository.delete(customer);
            return true;
        }).orElse(false);
    }
}
