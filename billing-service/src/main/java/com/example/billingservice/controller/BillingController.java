package com.example.billingservice.controller;

import com.example.billingservice.dto.request.CreateInvoiceRequest;
import com.example.billingservice.dto.request.ProcessPaymentRequest;
import com.example.billingservice.dto.response.CustomerSummaryResponse;
import com.example.billingservice.dto.response.InvoiceResponse;
import com.example.billingservice.dto.response.InvoiceStatusResponse;
import com.example.billingservice.dto.response.PaymentResponse;
import com.example.billingservice.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.billingservice.model.CustomerRecord;
import com.example.billingservice.model.BillRecord;
import com.example.billingservice.model.PaymentRecord;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

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

    @GetMapping("/customers")
    public List<CustomerRecord> getAllCustomers() {
        return billingService.getAllCustomers();
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerRecord> createCustomer(@Valid @RequestBody CustomerRecord customer) {
        CustomerRecord created = billingService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/customers/{id}")
    public CustomerRecord updateCustomer(@PathVariable long id, @Valid @RequestBody CustomerRecord customer) {
        return billingService.updateCustomer(id, customer);
    }

    @GetMapping("/invoices")
    public List<BillRecord> getAllBills() {
        return billingService.getAllBills();
    }

    @GetMapping("/payments")
    public List<PaymentRecord> getPaymentsForBill(@RequestParam long billId) {
        return billingService.getPaymentsForBill(billId);
    }
}
