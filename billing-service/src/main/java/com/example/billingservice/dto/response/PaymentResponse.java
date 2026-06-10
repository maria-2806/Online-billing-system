package com.example.billingservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long billId,
        BigDecimal amountPaid,
        BigDecimal remainingBalance,
        String status,
        LocalDateTime paymentDate
) {
}
