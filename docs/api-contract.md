# Online Billing System API Contract

This document is the working contract for the microservices in the Online Billing System.
It is the source of truth for request/response shapes, status codes, and endpoint responsibilities that affect Billing Service integration.

## Service Split

- Billing Service runs on port `8081` and owns business logic and orchestration.
- Data Pool Service runs on port `8080` and owns all database access.
- Billing Service must not access the database directly.

## Shared Conventions

- Base path for service APIs: `/api`
- JSON payloads use camelCase field names.
- Monetary values use `BigDecimal`-style decimal amounts, not floating point.
- Error responses should be consistent and machine-readable.

### Standard Error Response

```json
{
  "code": "CUSTOMER_NOT_FOUND",
  "message": "Customer with id 10 was not found"
}
```

## Data Pool Service API

Billing Service consumes these endpoints from Data Pool Service.

### Customers

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/customers` | Create a customer |
| `GET` | `/api/customers/{id}` | Fetch customer by id |
| `GET` | `/api/customers` | List customers |
| `PUT` | `/api/customers/{id}` | Update customer |

### Bills

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/bills` | Create a bill |
| `GET` | `/api/bills/{id}` | Fetch bill by id |
| `GET` | `/api/bills?customerId={id}` | List bills for a customer |
| `PUT` | `/api/bills/{id}` | Update bill or bill status |

### Payments

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/payments` | Record a payment |
| `GET` | `/api/payments/{id}` | Fetch payment by id |
| `GET` | `/api/payments?billId={id}` | List payments for a bill |

## Billing Service API

Billing Service exposes these endpoints to the client, UI, Postman, and future integrations.

### Create Invoice

`POST /api/billing/invoices`

Purpose: validate the customer, calculate billing totals, create the bill through Data Pool Service, and return an invoice response.

#### Request

```json
{
  "customerId": 1,
  "amount": 100.00
}
```

#### Success Response

`201 Created`

```json
{
  "billId": 100,
  "customerId": 1,
  "customerName": "John Doe",
  "subtotal": 100.00,
  "tax": 18.00,
  "total": 118.00,
  "status": "ISSUED"
}
```

### Process Payment

`POST /api/billing/payments`

Purpose: validate the bill, enforce payment rules, record the payment, and update bill status when fully paid.

#### Request

```json
{
  "billId": 100,
  "amount": 118.00,
  "method": "UPI"
}
```

#### Success Response

`200 OK`

```json
{
  "paymentId": 500,
  "billId": 100,
  "amountPaid": 118.00,
  "remainingBalance": 0.00,
  "status": "SUCCESS"
}
```

### Customer Summary

`GET /api/billing/customers/{id}/summary`

Purpose: return a billing summary for a customer, including invoice totals and counts by status.

#### Success Response

```json
{
  "customerId": 1,
  "customerName": "John Doe",
  "totalBills": 3,
  "paidBills": 2,
  "pendingBills": 1,
  "totalBilled": 354.00
}
```

### Invoice Status

`GET /api/billing/invoices/{id}/status`

Purpose: return the current invoice or bill status.

#### Success Response

```json
{
  "billId": 100,
  "status": "PAID"
}
```

## Canonical Status Values

### Bill Status

- `DRAFT`
- `ISSUED`
- `PARTIALLY_PAID`
- `PAID`
- `OVERDUE`
- `CANCELLED`

### Payment Status

- `PENDING`
- `SUCCESS`
- `FAILED`

## Integration Rules

- Billing Service should use mocked Data Pool responses while Data Pool is still being built.
- Any change to request or response fields must be reflected here before implementation changes are merged.
- If an endpoint is renamed, the consumer and provider must update together.
- Keep this document aligned with controller DTOs and client classes as the codebase evolves.
