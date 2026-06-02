package com.onlinebilling.billingservice.service;

import com.onlinebilling.billingservice.dto.request.CreateInvoiceRequest;
import com.onlinebilling.billingservice.dto.request.ProcessPaymentRequest;
import com.onlinebilling.billingservice.dto.response.CustomerSummaryResponse;
import com.onlinebilling.billingservice.dto.response.InvoiceResponse;
import com.onlinebilling.billingservice.dto.response.InvoiceStatusResponse;
import com.onlinebilling.billingservice.dto.response.PaymentResponse;

public interface BillingService {

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    PaymentResponse processPayment(ProcessPaymentRequest request);

    CustomerSummaryResponse getCustomerSummary(long customerId);

    InvoiceStatusResponse getInvoiceStatus(long billId);
}