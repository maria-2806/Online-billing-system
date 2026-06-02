package com.onlinebilling.billingservice.controller;

import com.onlinebilling.billingservice.dto.request.CreateInvoiceRequest;
import com.onlinebilling.billingservice.dto.request.ProcessPaymentRequest;
import com.onlinebilling.billingservice.dto.response.CustomerSummaryResponse;
import com.onlinebilling.billingservice.dto.response.InvoiceResponse;
import com.onlinebilling.billingservice.dto.response.InvoiceStatusResponse;
import com.onlinebilling.billingservice.dto.response.PaymentResponse;
import com.onlinebilling.billingservice.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = billingService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/payments")
    public PaymentResponse processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        return billingService.processPayment(request);
    }

    @GetMapping("/customers/{id}/summary")
    public CustomerSummaryResponse getCustomerSummary(@PathVariable long id) {
        return billingService.getCustomerSummary(id);
    }

    @GetMapping("/invoices/{id}/status")
    public InvoiceStatusResponse getInvoiceStatus(@PathVariable long id) {
        return billingService.getInvoiceStatus(id);
    }
}