package com.example.datapoolservice;

import com.example.datapoolservice.controller.PaymentController;
import com.example.datapoolservice.model.Bill;
import com.example.datapoolservice.model.Payment;
import com.example.datapoolservice.service.PaymentService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PaymentService paymentService() {
            return Mockito.mock(PaymentService.class);
        }
    }

    @Test
    public void testCreatePayment() throws Exception {
        Bill bill = Bill.builder().id(10L).build();
        Payment payment = Payment.builder()
                .amount(new java.math.BigDecimal("100.00"))
                .method("CREDIT_CARD")
                .status("SUCCESS")
                .build();

        Payment savedPayment = Payment.builder()
                .id(100L)
                .bill(bill)
                .amount(new java.math.BigDecimal("100.00"))
                .method("CREDIT_CARD")
                .status("SUCCESS")
                .build();

        when(paymentService.createPayment(eq(10L), any(Payment.class))).thenReturn(savedPayment);

        mockMvc.perform(post("/api/payments")
                        .param("billId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.method").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    public void testGetPaymentById() throws Exception {
        Payment payment = Payment.builder()
                .id(100L)
                .amount(new java.math.BigDecimal("100.00"))
                .method("CREDIT_CARD")
                .status("SUCCESS")
                .build();

        when(paymentService.getPaymentById(100L)).thenReturn(Optional.of(payment));

        mockMvc.perform(get("/api/payments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
