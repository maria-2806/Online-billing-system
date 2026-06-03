package com.example.billingservice.dto.response;

public record ErrorResponse(
        String code,
        String message
) {
}
