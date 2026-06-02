package com.onlinebilling.billingservice.dto.response;

public record InvoiceStatusResponse(
        Long billId,
        String status
) {
}