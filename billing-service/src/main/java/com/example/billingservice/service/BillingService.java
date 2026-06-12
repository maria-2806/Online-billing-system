package com.example.billingservice.service;

import com.example.billingservice.dto.request.CreateInvoiceRequest;
import com.example.billingservice.dto.request.ProcessPaymentRequest;
import com.example.billingservice.dto.response.CustomerSummaryResponse;
import com.example.billingservice.dto.response.InvoiceResponse;
import com.example.billingservice.dto.response.InvoiceStatusResponse;
import com.example.billingservice.dto.response.PaymentResponse;

import com.example.billingservice.model.CustomerRecord;
import com.example.billingservice.model.BillRecord;
import com.example.billingservice.model.PaymentRecord;
import java.util.List;

public interface BillingService {

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    PaymentResponse processPayment(ProcessPaymentRequest request);

    CustomerSummaryResponse getCustomerSummary(long customerId);

    InvoiceStatusResponse getInvoiceStatus(long billId);

    List<CustomerRecord> getAllCustomers();

    CustomerRecord getCustomer(long id);

    CustomerRecord createCustomer(CustomerRecord customer);

    CustomerRecord updateCustomer(long id, CustomerRecord customer);

    List<BillRecord> getAllBills();

    List<PaymentRecord> getPaymentsForBill(Long billId);
}
