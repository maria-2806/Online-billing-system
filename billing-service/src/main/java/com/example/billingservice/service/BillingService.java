package com.example.billingservice.service;

import com.example.billingservice.dto.request.CreateInvoiceRequest;
import com.example.billingservice.dto.request.ProcessPaymentRequest;
import com.example.billingservice.dto.response.CustomerSummaryResponse;
import com.example.billingservice.dto.response.InvoiceResponse;
import com.example.billingservice.dto.response.InvoiceStatusResponse;
import com.example.billingservice.dto.response.PaymentResponse;

public interface BillingService {

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    PaymentResponse processPayment(ProcessPaymentRequest request);

    CustomerSummaryResponse getCustomerSummary(long customerId);

    InvoiceStatusResponse getInvoiceStatus(long billId);
}
