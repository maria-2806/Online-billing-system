package com.example.billing_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class BillingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingSystemApplication.class, args);
	}

}
