package com.onlinebilling.billingservice.client;

import com.onlinebilling.billingservice.exception.DomainException;
import com.onlinebilling.billingservice.model.BillRecord;
import com.onlinebilling.billingservice.model.BillStatus;
import com.onlinebilling.billingservice.model.CustomerRecord;
import com.onlinebilling.billingservice.model.PaymentRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryDataPoolClient implements DataPoolClient {

    private final Map<Long, CustomerRecord> customers = new LinkedHashMap<>();
    private final Map<Long, BillRecord> bills = new LinkedHashMap<>();
    private final Map<Long, PaymentRecord> payments = new LinkedHashMap<>();
    private final AtomicLong billSequence = new AtomicLong(100L);
    private final AtomicLong paymentSequence = new AtomicLong(500L);

    public InMemoryDataPoolClient() {
        customers.put(1L, new CustomerRecord(1L, "John Doe", "john.doe@example.com"));
        customers.put(2L, new CustomerRecord(2L, "Jane Smith", "jane.smith@example.com"));
    }

    @Override
    public CustomerRecord getCustomer(long customerId) {
        CustomerRecord customerRecord = customers.get(customerId);
        if (customerRecord == null) {
            throw new DomainException("CUSTOMER_NOT_FOUND", "Customer with id %s was not found".formatted(customerId));
        }
        return customerRecord;
    }

    @Override
    public BillRecord getBill(long billId) {
        BillRecord billRecord = bills.get(billId);
        if (billRecord == null) {
            throw new DomainException("BILL_NOT_FOUND", "Bill with id %s was not found".formatted(billId));
        }
        return billRecord;
    }

    @Override
    public BillRecord saveBill(BillRecord billRecord) {
        long billId = billSequence.getAndIncrement();
        BillRecord storedBill = new BillRecord(
                billId,
                billRecord.customerId(),
                billRecord.customerName(),
                billRecord.subtotal(),
                billRecord.tax(),
                billRecord.total(),
                billRecord.status()
        );
        bills.put(billId, storedBill);
        return storedBill;
    }

    @Override
    public BillRecord updateBillStatus(long billId, BillStatus billStatus) {
        BillRecord existingBill = getBill(billId);
        BillRecord updatedBill = new BillRecord(
                existingBill.id(),
                existingBill.customerId(),
                existingBill.customerName(),
                existingBill.subtotal(),
                existingBill.tax(),
                existingBill.total(),
                billStatus
        );
        bills.put(billId, updatedBill);
        return updatedBill;
    }

    @Override
    public List<BillRecord> getBillsForCustomer(long customerId) {
        List<BillRecord> customerBills = new ArrayList<>();
        for (BillRecord billRecord : bills.values()) {
            if (billRecord.customerId() == customerId) {
                customerBills.add(billRecord);
            }
        }
        return customerBills;
    }

    @Override
    public PaymentRecord savePayment(PaymentRecord paymentRecord) {
        long paymentId = paymentSequence.getAndIncrement();
        PaymentRecord storedPayment = new PaymentRecord(
                paymentId,
                paymentRecord.billId(),
                paymentRecord.amountPaid(),
                paymentRecord.method(),
                paymentRecord.status()
        );
        payments.put(paymentId, storedPayment);
        return storedPayment;
    }

    @Override
    public List<PaymentRecord> getPaymentsForBill(long billId) {
        List<PaymentRecord> billPayments = new ArrayList<>();
        for (PaymentRecord paymentRecord : payments.values()) {
            if (paymentRecord.billId() == billId) {
                billPayments.add(paymentRecord);
            }
        }
        return billPayments;
    }
}