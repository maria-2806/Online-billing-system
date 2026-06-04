package com.example.billingservice.client;

import com.example.billingservice.exception.DomainException;
import com.example.billingservice.model.BillRecord;
import com.example.billingservice.model.BillStatus;
import com.example.billingservice.model.CustomerRecord;
import com.example.billingservice.model.PaymentRecord;
import com.example.billingservice.model.PaymentStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Profile("!mock")
@Component
public class RestDataPoolClient implements DataPoolClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestDataPoolClient(RestTemplate restTemplate, @Value("${datapool.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // Helper records for JSON serialization/deserialization to avoid importing datapool-service entities
    private record CustomerDto(Long id, String name, String email) {}

    private record BillDto(Long id, CustomerDto customer, BigDecimal amount, String status) {}

    private record BillIdDto(Long id) {}

    private record PaymentDto(Long id, BillIdDto bill, BigDecimal amount, String method, String status) {}

    private CustomerRecord toCustomerRecord(CustomerDto dto) {
        if (dto == null) return null;
        return new CustomerRecord(dto.id(), dto.name(), dto.email());
    }

    private BillRecord toBillRecord(BillDto dto) {
        if (dto == null) return null;
        
        BigDecimal total = dto.amount() != null ? dto.amount() : BigDecimal.ZERO;
        // Back-calculate subtotal and tax based on total (total = subtotal * 1.18)
        BigDecimal subtotal = total.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);
        
        Long customerId = dto.customer() != null ? dto.customer().id() : null;
        String customerName = dto.customer() != null ? dto.customer().name() : null;
        
        return new BillRecord(
                dto.id(),
                customerId,
                customerName,
                subtotal,
                tax,
                total,
                mapStatusToRecord(dto.status())
        );
    }

    private PaymentRecord toPaymentRecord(PaymentDto dto) {
        if (dto == null) return null;
        Long billId = dto.bill() != null ? dto.bill().id() : null;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        if (dto.status() != null) {
            try {
                paymentStatus = PaymentStatus.valueOf(dto.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Keep default PENDING
            }
        }
        return new PaymentRecord(
                dto.id(),
                billId,
                dto.amount(),
                dto.method(),
                paymentStatus
        );
    }

    private BillStatus mapStatusToRecord(String status) {
        if (status == null) {
            return BillStatus.ISSUED;
        }
        switch (status.toUpperCase()) {
            case "PENDING":
            case "ISSUED":
                return BillStatus.ISSUED;
            case "PAID":
                return BillStatus.PAID;
            case "CANCELLED":
                return BillStatus.CANCELLED;
            case "DRAFT":
                return BillStatus.DRAFT;
            case "OVERDUE":
                return BillStatus.OVERDUE;
            case "PARTIALLY_PAID":
                return BillStatus.PARTIALLY_PAID;
            default:
                return BillStatus.ISSUED;
        }
    }

    private String mapStatusToEntity(BillStatus status) {
        if (status == null) {
            return "PENDING";
        }
        switch (status) {
            case ISSUED:
                return "PENDING";
            case PAID:
                return "PAID";
            case CANCELLED:
                return "CANCELLED";
            case PARTIALLY_PAID:
                return "PARTIALLY_PAID";
            default:
                return status.name();
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "getCustomerFallback")
    public CustomerRecord getCustomer(long customerId) {
        try {
            CustomerDto dto = restTemplate.getForObject(baseUrl + "/customers/" + customerId, CustomerDto.class);
            if (dto == null) {
                throw new DomainException("CUSTOMER_NOT_FOUND", "Customer with id " + customerId + " was not found");
            }
            return toCustomerRecord(dto);
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("CUSTOMER_NOT_FOUND", "Customer with id " + customerId + " was not found");
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "getBillFallback")
    public BillRecord getBill(long billId) {
        try {
            BillDto dto = restTemplate.getForObject(baseUrl + "/bills/" + billId, BillDto.class);
            if (dto == null) {
                throw new DomainException("BILL_NOT_FOUND", "Bill with id " + billId + " was not found");
            }
            return toBillRecord(dto);
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("BILL_NOT_FOUND", "Bill with id " + billId + " was not found");
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "saveBillFallback")
    public BillRecord saveBill(BillRecord billRecord) {
        String url = baseUrl + "/bills?customerId=" + billRecord.customerId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        BillDto requestBody = new BillDto(
                null,
                null,
                billRecord.total(),
                mapStatusToEntity(billRecord.status())
        );
        
        HttpEntity<BillDto> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            BillDto dto = restTemplate.postForObject(url, requestEntity, BillDto.class);
            return toBillRecord(dto);
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("CUSTOMER_NOT_FOUND", "Customer with id " + billRecord.customerId() + " was not found");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new DomainException("CUSTOMER_NOT_FOUND", "Customer with id " + billRecord.customerId() + " was not found");
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "updateBillStatusFallback")
    public BillRecord updateBillStatus(long billId, BillStatus billStatus) {
        String url = baseUrl + "/bills/" + billId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        BillDto requestBody = new BillDto(
                null,
                null,
                null,
                mapStatusToEntity(billStatus)
        );
        
        HttpEntity<BillDto> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<BillDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    BillDto.class
            );
            return toBillRecord(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("BILL_NOT_FOUND", "Bill with id " + billId + " was not found");
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "getBillsForCustomerFallback")
    public List<BillRecord> getBillsForCustomer(long customerId) {
        String url = baseUrl + "/bills?customerId=" + customerId;
        try {
            BillDto[] dtos = restTemplate.getForObject(url, BillDto[].class);
            if (dtos == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(dtos)
                    .map(dto -> toBillRecord(dto))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "savePaymentFallback")
    public PaymentRecord savePayment(PaymentRecord paymentRecord) {
        String url = baseUrl + "/payments?billId=" + paymentRecord.billId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        PaymentDto requestBody = new PaymentDto(
                null,
                null,
                paymentRecord.amountPaid(),
                paymentRecord.method(),
                paymentRecord.status() != null ? paymentRecord.status().name() : "PENDING"
        );
        
        HttpEntity<PaymentDto> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            PaymentDto dto = restTemplate.postForObject(url, requestEntity, PaymentDto.class);
            return toPaymentRecord(dto);
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("BILL_NOT_FOUND", "Bill with id " + paymentRecord.billId() + " was not found");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new DomainException("BILL_NOT_FOUND", "Bill with id " + paymentRecord.billId() + " was not found");
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "getPaymentsForBillFallback")
    public List<PaymentRecord> getPaymentsForBill(long billId) {
        String url = baseUrl + "/payments?billId=" + billId;
        try {
            PaymentDto[] dtos = restTemplate.getForObject(url, PaymentDto[].class);
            if (dtos == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(dtos)
                    .map(dto -> toPaymentRecord(dto))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // --- Fallback Methods ---

    public CustomerRecord getCustomerFallback(long customerId, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public BillRecord getBillFallback(long billId, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public BillRecord saveBillFallback(BillRecord billRecord, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public BillRecord updateBillStatusFallback(long billId, BillStatus billStatus, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public List<BillRecord> getBillsForCustomerFallback(long customerId, Throwable t) {
        handleFallbackException(t);
        return Collections.emptyList();
    }

    public PaymentRecord savePaymentFallback(PaymentRecord paymentRecord, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public List<PaymentRecord> getPaymentsForBillFallback(long billId, Throwable t) {
        handleFallbackException(t);
        return Collections.emptyList();
    }

    private void handleFallbackException(Throwable t) {
        if (t instanceof DomainException) {
            throw (DomainException) t;
        }
        throw new DomainException(
                "DATABASE_SERVICE_UNAVAILABLE",
                "Data Pool Service is temporarily offline. Details: " + t.getMessage()
        );
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "getAllCustomersFallback")
    public List<CustomerRecord> getAllCustomers() {
        String url = baseUrl + "/customers";
        try {
            CustomerDto[] dtos = restTemplate.getForObject(url, CustomerDto[].class);
            if (dtos == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(dtos)
                    .map(this::toCustomerRecord)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "saveCustomerFallback")
    public CustomerRecord saveCustomer(CustomerRecord customerRecord) {
        String url = baseUrl + "/customers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        CustomerDto requestBody = new CustomerDto(null, customerRecord.name(), customerRecord.email());
        HttpEntity<CustomerDto> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            CustomerDto dto = restTemplate.postForObject(url, requestEntity, CustomerDto.class);
            return toCustomerRecord(dto);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new DomainException("INVALID_CUSTOMER_DATA", "Failed to save customer: " + e.getResponseBodyAsString());
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "updateCustomerFallback")
    public CustomerRecord updateCustomer(long customerId, CustomerRecord customerRecord) {
        String url = baseUrl + "/customers/" + customerId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        CustomerDto requestBody = new CustomerDto(null, customerRecord.name(), customerRecord.email());
        HttpEntity<CustomerDto> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<CustomerDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    CustomerDto.class
            );
            return toCustomerRecord(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException("CUSTOMER_NOT_FOUND", "Customer with id " + customerId + " was not found");
        }
    }

    @Override
    @CircuitBreaker(name = "datapoolClient", fallbackMethod = "getAllBillsFallback")
    public List<BillRecord> getAllBills() {
        String url = baseUrl + "/bills";
        try {
            BillDto[] dtos = restTemplate.getForObject(url, BillDto[].class);
            if (dtos == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(dtos)
                    .map(dto -> toBillRecord(dto))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // --- New Fallback Methods ---

    public List<CustomerRecord> getAllCustomersFallback(Throwable t) {
        handleFallbackException(t);
        return Collections.emptyList();
    }

    public CustomerRecord saveCustomerFallback(CustomerRecord customerRecord, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public CustomerRecord updateCustomerFallback(long customerId, CustomerRecord customerRecord, Throwable t) {
        handleFallbackException(t);
        return null;
    }

    public List<BillRecord> getAllBillsFallback(Throwable t) {
        handleFallbackException(t);
        return Collections.emptyList();
    }
}
