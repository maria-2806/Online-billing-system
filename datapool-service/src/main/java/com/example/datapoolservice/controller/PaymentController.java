package com.example.datapoolservice.controller;

import com.example.datapoolservice.model.Payment;
import com.example.datapoolservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestParam Long billId, @RequestBody Payment payment) {
        try {
            Payment created = paymentService.createPayment(billId, payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getPayments(@RequestParam(required = false) Long billId) {
        return ResponseEntity.ok(paymentService.getPayments(billId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Payment> updatePaymentStatus(@PathVariable Long id, @RequestParam String status) {
        return paymentService.updatePaymentStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
