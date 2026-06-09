package com.example.billingservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCustomerRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "invalid email format")
        String email,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "invalid phone number format")
        String phone
) {
}
