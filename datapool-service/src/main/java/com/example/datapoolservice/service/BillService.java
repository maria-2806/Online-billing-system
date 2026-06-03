package com.example.datapoolservice.service;

import com.example.datapoolservice.model.Bill;
import com.example.datapoolservice.model.Customer;
import com.example.datapoolservice.repository.BillRepository;
import com.example.datapoolservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Bill createBill(Long customerId, Bill bill) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));
        bill.setCustomer(customer);
        if (bill.getStatus() == null) {
            bill.setStatus("PENDING");
        }
        return billRepository.save(bill);
    }

    public Optional<Bill> getBillById(Long id) {
        return billRepository.findById(id);
    }

    public List<Bill> getBills(Long customerId, String status) {
        if (customerId != null && status != null) {
            return billRepository.findByCustomerIdAndStatus(customerId, status);
        } else if (customerId != null) {
            return billRepository.findByCustomerId(customerId);
        } else if (status != null) {
            return billRepository.findByStatus(status);
        } else {
            return billRepository.findAll();
        }
    }

    @Transactional
    public Optional<Bill> updateBill(Long id, Bill billDetails) {
        return billRepository.findById(id).map(bill -> {
            if (billDetails.getAmount() != null) {
                bill.setAmount(billDetails.getAmount());
            }
            if (billDetails.getStatus() != null) {
                bill.setStatus(billDetails.getStatus());
            }
            return billRepository.save(bill);
        });
    }

    @Transactional
    public boolean deleteBill(Long id) {
        return billRepository.findById(id).map(bill -> {
            billRepository.delete(bill);
            return true;
        }).orElse(false);
    }
}
