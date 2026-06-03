package com.example.datapoolservice.service;

import com.example.datapoolservice.model.Bill;
import com.example.datapoolservice.model.Payment;
import com.example.datapoolservice.repository.BillRepository;
import com.example.datapoolservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;

    @Transactional
    public Payment createPayment(Long billId, Payment payment) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found with id: " + billId));
        payment.setBill(bill);
        if (payment.getStatus() == null) {
            payment.setStatus("PENDING");
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        
        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            bill.setStatus("PAID");
            billRepository.save(bill);
        }
        
        return savedPayment;
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getPayments(Long billId) {
        if (billId != null) {
            return paymentRepository.findByBillId(billId);
        }
        return paymentRepository.findAll();
    }

    @Transactional
    public Optional<Payment> updatePaymentStatus(Long id, String status) {
        return paymentRepository.findById(id).map(payment -> {
            payment.setStatus(status);
            Payment saved = paymentRepository.save(payment);
            
            if ("SUCCESS".equalsIgnoreCase(status)) {
                Bill bill = payment.getBill();
                bill.setStatus("PAID");
                billRepository.save(bill);
            }
            return saved;
        });
    }
}
