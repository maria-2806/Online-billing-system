package com.example.billingservice.dto.response;

public record InvoiceStatusResponse(
        Long billId,
        String status
) {
}
