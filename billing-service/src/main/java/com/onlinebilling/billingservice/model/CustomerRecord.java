package com.onlinebilling.billingservice.model;

public record CustomerRecord(
        Long id,
        String name,
        String email
) {
}