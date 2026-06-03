package com.example.billingservice.service;

import com.example.billingservice.client.DataPoolClient;
import com.example.billingservice.dto.request.CreateInvoiceRequest;
import com.example.billingservice.dto.request.ProcessPaymentRequest;
import com.example.billingservice.dto.response.CustomerSummaryResponse;
import com.example.billingservice.dto.response.InvoiceResponse;
import com.example.billingservice.dto.response.InvoiceStatusResponse;
import com.example.billingservice.dto.response.PaymentResponse;
import com.example.billingservice.exception.DomainException;
import com.example.billingservice.model.BillRecord;
import com.example.billingservice.model.BillStatus;
import com.example.billingservice.model.CustomerRecord;
import com.example.billingservice.model.PaymentRecord;
import com.example.billingservice.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BillingServiceImpl implements BillingService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");

    private final DataPoolClient dataPoolClient;

    public BillingServiceImpl(DataPoolClient dataPoolClient) {
        this.dataPoolClient = dataPoolClient;
    }

    @Override
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        CustomerRecord customer = dataPoolClient.getCustomer(request.customerId());
        BigDecimal subtotal = scaleMoney(request.amount());
        BigDecimal tax = scaleMoney(subtotal.multiply(TAX_RATE));
        BigDecimal total = scaleMoney(subtotal.add(tax));

        BillRecord savedBill = dataPoolClient.saveBill(new BillRecord(
                null,
                customer.id(),
                customer.name(),
                subtotal,
                tax,
                total,
                BillStatus.ISSUED
        ));

        return new InvoiceResponse(
                savedBill.id(),
                savedBill.customerId(),
                savedBill.customerName(),
                savedBill.subtotal(),
                savedBill.tax(),
                savedBill.total(),
                savedBill.status().name()
        );
    }

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        BillRecord bill = dataPoolClient.getBill(request.billId());
        
        if (bill.status() == BillStatus.PAID) {
            throw new DomainException(
                    "BILL_ALREADY_PAID",
                    "This bill has already been fully paid"
            );
        }
        if (bill.status() == BillStatus.CANCELLED) {
            throw new DomainException(
                    "BILL_CANCELLED",
                    "Cannot process payment for a cancelled bill"
            );
        }

        BigDecimal paymentAmount = scaleMoney(request.amount());
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(
                    "INVALID_PAYMENT_AMOUNT",
                    "Payment amount must be greater than zero"
            );
        }

        BigDecimal paidSoFar = sumPayments(dataPoolClient.getPaymentsForBill(bill.id()));
        BigDecimal remainingBeforePayment = scaleMoney(bill.total().subtract(paidSoFar));

        if (paymentAmount.compareTo(remainingBeforePayment) > 0) {
            throw new DomainException(
                    "PAYMENT_EXCEEDS_BALANCE",
                    "Payment amount cannot exceed the remaining balance"
            );
        }

        PaymentRecord storedPayment = dataPoolClient.savePayment(new PaymentRecord(
                null,
                bill.id(),
                paymentAmount,
                request.method(),
                PaymentStatus.SUCCESS
        ));

        BigDecimal remainingAfterPayment = scaleMoney(remainingBeforePayment.subtract(paymentAmount));
        if (remainingAfterPayment.compareTo(BigDecimal.ZERO) == 0) {
            dataPoolClient.updateBillStatus(bill.id(), BillStatus.PAID);
        } else {
            dataPoolClient.updateBillStatus(bill.id(), BillStatus.PARTIALLY_PAID);
        }

        return new PaymentResponse(
                storedPayment.id(),
                bill.id(),
                paymentAmount,
                remainingAfterPayment,
                storedPayment.status().name()
        );
    }

    @Override
    public CustomerSummaryResponse getCustomerSummary(long customerId) {
        CustomerRecord customer = dataPoolClient.getCustomer(customerId);
        List<BillRecord> bills = dataPoolClient.getBillsForCustomer(customerId);

        long paidBills = bills.stream().filter(bill -> bill.status() == BillStatus.PAID).count();
        long pendingBills = bills.stream().filter(bill -> bill.status() != BillStatus.PAID && bill.status() != BillStatus.CANCELLED).count();
        BigDecimal totalBilled = bills.stream()
                .map(BillRecord::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerSummaryResponse(
                customer.id(),
                customer.name(),
                bills.size(),
                paidBills,
                pendingBills,
                scaleMoney(totalBilled)
        );
    }

    @Override
    public InvoiceStatusResponse getInvoiceStatus(long billId) {
        BillRecord bill = dataPoolClient.getBill(billId);
        return new InvoiceStatusResponse(bill.id(), bill.status().name());
    }

    private BigDecimal sumPayments(List<PaymentRecord> paymentRecords) {
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentRecord paymentRecord : paymentRecords) {
            if (paymentRecord.status() == PaymentStatus.SUCCESS) {
                total = total.add(paymentRecord.amountPaid());
            }
        }
        return scaleMoney(total);
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
