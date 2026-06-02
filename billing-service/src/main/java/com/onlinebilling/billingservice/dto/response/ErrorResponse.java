package com.onlinebilling.billingservice.dto.response;

public record ErrorResponse(
        String code,
        String message
) {
}