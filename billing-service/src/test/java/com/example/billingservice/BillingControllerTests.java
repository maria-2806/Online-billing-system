package com.example.billingservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("mock")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BillingControllerTests {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void createInvoiceReturnsCalculatedTotals() throws Exception {
        HttpResponse<String> response = sendPost("/api/billing/invoices", """
                {
                  "customerId": 1,
                  "amount": 100.00
                }
                """);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"customerId\":1");
        assertThat(response.body()).contains("\"customerName\":\"John Doe\"");
        assertThat(response.body()).contains("\"status\":\"ISSUED\"");
        assertThat(extractBigDecimal(response.body(), "subtotal")).isEqualByComparingTo("100.00");
        assertThat(extractBigDecimal(response.body(), "tax")).isEqualByComparingTo("18.00");
        assertThat(extractBigDecimal(response.body(), "total")).isEqualByComparingTo("118.00");
    }

    @Test
    void processPaymentMarksInvoicePaidWhenFullySettled() throws Exception {
        HttpResponse<String> invoiceResponse = sendPost("/api/billing/invoices", """
                {
                  "customerId": 2,
                  "amount": 250.00
                }
                """);

        long billId = extractLong(invoiceResponse.body(), "billId");

        HttpResponse<String> paymentResponse = sendPost("/api/billing/payments", """
                {
                  "billId": %d,
                  "amount": 295.00,
                  "method": "UPI"
                }
                """.formatted(billId));

        assertThat(paymentResponse.statusCode()).isEqualTo(200);
        assertThat(paymentResponse.body()).contains("\"status\":\"SUCCESS\"");
        assertThat(extractBigDecimal(paymentResponse.body(), "amountPaid")).isEqualByComparingTo("295.00");
        assertThat(extractBigDecimal(paymentResponse.body(), "remainingBalance")).isEqualByComparingTo("0.00");

        HttpResponse<String> statusResponse = sendGet("/api/billing/invoices/%d/status".formatted(billId));
        assertThat(statusResponse.statusCode()).isEqualTo(200);
        assertThat(statusResponse.body()).contains("\"status\":\"PAID\"");
    }

    @Test
    void customerSummaryReflectsCreatedInvoicesAndPayments() throws Exception {
        HttpResponse<String> invoiceResponse = sendPost("/api/billing/invoices", """
                {
                  "customerId": 1,
                  "amount": 150.00
                }
                """);

        long billId = extractLong(invoiceResponse.body(), "billId");

        HttpResponse<String> paymentResponse = sendPost("/api/billing/payments", """
                {
                  "billId": %d,
                  "amount": 177.00,
                  "method": "CARD"
                }
                """.formatted(billId));

        assertThat(paymentResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> summaryResponse = sendGet("/api/billing/customers/1/summary");
        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        assertThat(summaryResponse.body()).contains("\"customerName\":\"John Doe\"");
        assertThat(extractLong(summaryResponse.body(), "totalBills")).isEqualTo(1L);
        assertThat(extractLong(summaryResponse.body(), "paidBills")).isEqualTo(1L);
        assertThat(extractLong(summaryResponse.body(), "pendingBills")).isEqualTo(0L);
        assertThat(extractBigDecimal(summaryResponse.body(), "totalBilled")).isEqualByComparingTo("177.00");
    }

    @Test
    void partialPaymentsTransitionStatusCorrectly() throws Exception {
        HttpResponse<String> invoiceResponse = sendPost("/api/billing/invoices", """
                {
                  "customerId": 1,
                  "amount": 100.00
                }
                """);

        long billId = extractLong(invoiceResponse.body(), "billId");

        HttpResponse<String> paymentResponse1 = sendPost("/api/billing/payments", """
                {
                  "billId": %d,
                  "amount": 50.00,
                  "method": "CARD"
                }
                """.formatted(billId));

        assertThat(paymentResponse1.statusCode()).isEqualTo(200);
        assertThat(paymentResponse1.body()).contains("\"status\":\"SUCCESS\"");
        assertThat(extractBigDecimal(paymentResponse1.body(), "amountPaid")).isEqualByComparingTo("50.00");
        assertThat(extractBigDecimal(paymentResponse1.body(), "remainingBalance")).isEqualByComparingTo("68.00");

        HttpResponse<String> statusResponse1 = sendGet("/api/billing/invoices/%d/status".formatted(billId));
        assertThat(statusResponse1.statusCode()).isEqualTo(200);
        assertThat(statusResponse1.body()).contains("\"status\":\"PARTIALLY_PAID\"");

        HttpResponse<String> paymentResponse2 = sendPost("/api/billing/payments", """
                {
                  "billId": %d,
                  "amount": 68.00,
                  "method": "UPI"
                }
                """.formatted(billId));

        assertThat(paymentResponse2.statusCode()).isEqualTo(200);
        assertThat(extractBigDecimal(paymentResponse2.body(), "remainingBalance")).isEqualByComparingTo("0.00");

        HttpResponse<String> statusResponse2 = sendGet("/api/billing/invoices/%d/status".formatted(billId));
        assertThat(statusResponse2.statusCode()).isEqualTo(200);
        assertThat(statusResponse2.body()).contains("\"status\":\"PAID\"");

        HttpResponse<String> paymentResponse3 = sendPost("/api/billing/payments", """
                {
                  "billId": %d,
                  "amount": 10.00,
                  "method": "CARD"
                }
                """.formatted(billId));
        assertThat(paymentResponse3.statusCode()).isEqualTo(400);
        assertThat(paymentResponse3.body()).contains("\"code\":\"BILL_ALREADY_PAID\"");
    }

    @Test
    void customerCreationValidatesNameNoNumbers() throws Exception {
        HttpResponse<String> response = sendPost("/api/billing/customers", """
                {
                  "name": "John123 Doe",
                  "email": "john.doe@gmail.com",
                  "phone": "1234567890"
                }
                """);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"");
        assertThat(response.body()).contains("name must contain only alphabetic characters");
    }

    @Test
    void customerCreationValidatesGmailOnly() throws Exception {
        HttpResponse<String> response = sendPost("/api/billing/customers", """
                {
                  "name": "John Doe",
                  "email": "john.doe@yahoo.com",
                  "phone": "1234567890"
                }
                """);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"");
        assertThat(response.body()).contains("email must be a valid Gmail address");
    }

    @Test
    void customerCreationValidatesGmailHasLetters() throws Exception {
        HttpResponse<String> response = sendPost("/api/billing/customers", """
                {
                  "name": "John Doe",
                  "email": "123@gmail.com",
                  "phone": "1234567890"
                }
                """);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"");
        assertThat(response.body()).contains("email must be a valid Gmail address containing at least one letter");
    }
    
    @Test
    void customerCreationPassesWithValidData() throws Exception {
        HttpResponse<String> response = sendPost("/api/billing/customers", """
                {
                  "name": "Jane Doe",
                  "email": "jane.doe@gmail.com",
                  "phone": "1234567890"
                }
                """);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"name\":\"Jane Doe\"");
        assertThat(response.body()).contains("\"email\":\"jane.doe@gmail.com\"");
        assertThat(response.body()).contains("\"phone\":\"1234567890\"");
    }

    private HttpResponse<String> sendPost(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private long extractLong(String body, String fieldName) {
        Pattern pattern = Pattern.compile("\\\"" + fieldName + "\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to extract field: " + fieldName + " from body: " + body);
        }
        return Long.parseLong(matcher.group(1));
    }

    private BigDecimal extractBigDecimal(String body, String fieldName) {
        Pattern pattern = Pattern.compile("\\\"" + fieldName + "\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to extract field: " + fieldName + " from body: " + body);
        }
        return new BigDecimal(matcher.group(1));
    }
}
