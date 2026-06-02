package com.onlinebilling.billingservice.client;

import com.onlinebilling.billingservice.model.BillRecord;
import com.onlinebilling.billingservice.model.BillStatus;
import com.onlinebilling.billingservice.model.CustomerRecord;
import com.onlinebilling.billingservice.model.PaymentRecord;

import java.util.List;

public interface DataPoolClient {

    CustomerRecord getCustomer(long customerId);

    BillRecord getBill(long billId);

    BillRecord saveBill(BillRecord billRecord);

    BillRecord updateBillStatus(long billId, BillStatus billStatus);

    List<BillRecord> getBillsForCustomer(long customerId);

    PaymentRecord savePayment(PaymentRecord paymentRecord);

    List<PaymentRecord> getPaymentsForBill(long billId);
}