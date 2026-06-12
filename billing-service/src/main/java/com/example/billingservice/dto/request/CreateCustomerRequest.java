package com.example.billingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCustomerRequest(
        @NotBlank(message = "name is required")
        @Pattern(regexp = "^[a-zA-Z\\s'.\\-]+$", message = "name must contain only alphabetic characters, spaces, hyphens, or apostrophes (no numbers)")
        String name,

        @NotBlank(message = "email is required")
        @Pattern(regexp = "^(?=[0-9.]*[a-zA-Z])[a-zA-Z0-9.]+@gmail\\.com$", message = "email must be a valid Gmail address containing at least one letter")
        String email,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "phone number must be exactly 10 digits")
        String phone
) {
}
