package com.example.billingservice.client;

import com.example.billingservice.model.BillRecord;
import com.example.billingservice.model.BillStatus;
import com.example.billingservice.model.CustomerRecord;
import com.example.billingservice.model.PaymentRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface DataPoolClient {

    CustomerRecord getCustomer(long customerId);

    BillRecord getBill(long billId);

    BillRecord saveBill(BillRecord billRecord);

    BillRecord updateBillStatus(long billId, BillStatus billStatus, LocalDateTime paidAt);

    List<BillRecord> getBillsForCustomer(long customerId);

    PaymentRecord savePayment(PaymentRecord paymentRecord);

    List<PaymentRecord> getPaymentsForBill(Long billId);

    List<CustomerRecord> getAllCustomers();

    CustomerRecord saveCustomer(CustomerRecord customerRecord);

    CustomerRecord updateCustomer(long customerId, CustomerRecord customerRecord);

    List<BillRecord> getAllBills();
}
