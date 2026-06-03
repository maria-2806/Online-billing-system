package com.example.datapoolservice;

import com.example.datapoolservice.controller.CustomerController;
import com.example.datapoolservice.model.Customer;
import com.example.datapoolservice.service.CustomerService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CustomerService customerService() {
            return Mockito.mock(CustomerService.class);
        }
    }

    @Test
    public void testCreateCustomer() throws Exception {
        Customer customer = Customer.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();
        
        Customer savedCustomer = Customer.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(customerService.createCustomer(any(Customer.class))).thenReturn(savedCustomer);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    public void testGetCustomerById() throws Exception {
        Customer customer = Customer.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(customerService.getCustomerById(1L)).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    public void testGetCustomerByIdNotFound() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isNotFound());
    }
}
