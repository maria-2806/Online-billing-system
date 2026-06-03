package com.example.datapoolservice;

import com.example.datapoolservice.controller.BillController;
import com.example.datapoolservice.model.Bill;
import com.example.datapoolservice.model.Customer;
import com.example.datapoolservice.service.BillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillController.class)
public class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillService billService;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public BillService billService() {
            return Mockito.mock(BillService.class);
        }
    }

    @Test
    public void testCreateBill() throws Exception {
        Customer customer = Customer.builder().id(1L).name("John").email("john@example.com").build();
        Bill bill = Bill.builder()
                .amount(new BigDecimal("150.00"))
                .status("PENDING")
                .build();
        
        Bill savedBill = Bill.builder()
                .id(10L)
                .customer(customer)
                .amount(new BigDecimal("150.00"))
                .status("PENDING")
                .build();

        when(billService.createBill(eq(1L), any(Bill.class))).thenReturn(savedBill);

        mockMvc.perform(post("/api/bills")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bill)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    public void testGetBillById() throws Exception {
        Bill bill = Bill.builder()
                .id(10L)
                .amount(new BigDecimal("150.00"))
                .status("PENDING")
                .build();

        when(billService.getBillById(10L)).thenReturn(Optional.of(bill));

        mockMvc.perform(get("/api/bills/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amount").value(150.00));
    }
}
