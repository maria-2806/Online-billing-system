# Data Pool Service - API Documentation

The Data Pool Service exposes REST API endpoints to perform CRUD and querying operations on Customers, Bills, and Payments. It interfaces directly with the local MySQL database.

## Base URL
By default, the service runs on port `8080`:
`http://localhost:8080`

All requests should use `Content-Type: application_json` for request bodies.

---

## 👥 Customer Endpoints

### 1. Create Customer
Creates a new customer record. Emails must be unique.

* **Method**: `POST`
* **Path**: `/api/customers`
* **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  }
  ```
* **Success Response**: `201 Created`
  ```json
  {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  }
  ```
* **Error Response**: `400 Bad Request` (when email already exists or inputs are invalid)
  ```
  Customer with email jane.doe@example.com already exists.
  ```

---

### 2. Get Customer by ID
Retrieves details of a customer by their unique identifier.

* **Method**: `GET`
* **Path**: `/api/customers/{id}`
* **Success Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane.doe@example.com"
  }
  ```
* **Error Response**: `404 Not Found` (when customer ID does not exist)

---

### 3. List All Customers
Retrieves a list of all registered customers.

* **Method**: `GET`
* **Path**: `/api/customers`
* **Success Response**: `200 OK`
  ```json
  [
    {
      "id": 1,
      "name": "Jane Doe",
      "email": "jane.doe@example.com"
    }
  ]
  ```

---

### 4. Update Customer
Updates details of an existing customer.

* **Method**: `PUT`
* **Path**: `/api/customers/{id}`
* **Request Body**:
  ```json
  {
    "name": "Jane Smith",
    "email": "jane.smith@example.com"
  }
  ```
* **Success Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "Jane Smith",
    "email": "jane.smith@example.com"
  }
  ```
* **Error Response**: `404 Not Found`

---

### 5. Delete Customer
Deletes a customer by ID (and cascades to delete their associated bills and payments).

* **Method**: `DELETE`
* **Path**: `/api/customers/{id}`
* **Success Response**: `240 No Content` -> `204 No Content`
* **Error Response**: `404 Not Found`

---

## 📄 Bill Endpoints

### 1. Create Bill
Creates a new bill for a customer.

* **Method**: `POST`
* **Path**: `/api/bills`
* **Query Parameters**:
  * `customerId` (Required, Long): The unique ID of the customer.
* **Request Body**:
  ```json
  {
    "amount": 250.00,
    "status": "PENDING"
  }
  ```
* **Success Response**: `201 Created`
  ```json
  {
    "id": 10,
    "amount": 250.00,
    "status": "PENDING",
    "customer": {
      "id": 1,
      "name": "Jane Smith",
      "email": "jane.smith@example.com"
    }
  }
  ```
* **Error Response**: `400 Bad Request` (when customer does not exist)
  ```
  Customer not found with id: 1
  ```

---

### 2. Get Bill by ID
Retrieves details of a bill by its unique identifier.

* **Method**: `GET`
* **Path**: `/api/bills/{id}`
* **Success Response**: `200 OK`
  ```json
  {
    "id": 10,
    "amount": 250.00,
    "status": "PENDING",
    "customer": {
      "id": 1,
      "name": "Jane Smith",
      "email": "jane.smith@example.com"
    }
  }
  ```
* **Error Response**: `404 Not Found`

---

### 3. List / Search Bills
Retrieves lists of bills. Supports filtering by customer or status.

* **Method**: `GET`
* **Path**: `/api/bills`
* **Query Parameters** (Optional):
  * `customerId` (Long): Filter bills by customer ID.
  * `status` (String): Filter bills by status (e.g. `PENDING`, `PAID`, `CANCELLED`).
* **Success Response**: `200 OK`
  ```json
  [
    {
      "id": 10,
      "amount": 250.00,
      "status": "PENDING",
      "customer": {
        "id": 1,
        "name": "Jane Smith",
        "email": "jane.smith@example.com"
      }
    }
  ]
  ```

---

### 4. Update Bill
Updates amount or status details of an existing bill.

* **Method**: `PUT`
* **Path**: `/api/bills/{id}`
* **Request Body**:
  ```json
  {
    "amount": 275.50,
    "status": "PAID"
  }
  ```
* **Success Response**: `200 OK`
  ```json
  {
    "id": 10,
    "amount": 275.50,
    "status": "PAID",
    "customer": {
      "id": 1,
      "name": "Jane Smith",
      "email": "jane.smith@example.com"
    }
  }
  ```
* **Error Response**: `404 Not Found`

---

### 5. Delete Bill
Deletes a bill by ID (cascades to delete related payments).

* **Method**: `DELETE`
* **Path**: `/api/bills/{id}`
* **Success Response**: `204 No Content`
* **Error Response**: `404 Not Found`

---

## 💳 Payment Endpoints

### 1. Create Payment
Records a payment transaction for an existing bill. 
*Note: If the recorded status is `SUCCESS`, the associated bill's status is automatically updated to `PAID` in the database.*

* **Method**: `POST`
* **Path**: `/api/payments`
* **Query Parameters**:
  * `billId` (Required, Long): The unique ID of the bill.
* **Request Body**:
  ```json
  {
    "amount": 275.50,
    "method": "CREDIT_CARD",
    "status": "SUCCESS"
  }
  ```
* **Success Response**: `201 Created`
  ```json
  {
    "id": 100,
    "amount": 275.50,
    "method": "CREDIT_CARD",
    "status": "SUCCESS",
    "bill": {
      "id": 10,
      "amount": 275.50,
      "status": "PAID"
    }
  }
  ```
* **Error Response**: `400 Bad Request` (when bill does not exist)
  ```
  Bill not found with id: 10
  ```

---

### 2. Get Payment by ID
Retrieves details of a payment transaction.

* **Method**: `GET`
* **Path**: `/api/payments/{id}`
* **Success Response**: `200 OK`
  ```json
  {
    "id": 100,
    "amount": 275.50,
    "method": "CREDIT_CARD",
    "status": "SUCCESS",
    "bill": {
      "id": 10,
      "amount": 275.50,
      "status": "PAID"
    }
  }
  ```
* **Error Response**: `404 Not Found`

---

### 3. List Payments
Lists all payment transactions. Supports filtering by bill ID.

* **Method**: `GET`
* **Path**: `/api/payments`
* **Query Parameters** (Optional):
  * `billId` (Long): Filter payments associated with a specific bill.
* **Success Response**: `200 OK`
  ```json
  [
    {
      "id": 100,
      "amount": 275.50,
      "method": "CREDIT_CARD",
      "status": "SUCCESS",
      "bill": {
        "id": 10,
        "amount": 275.50,
        "status": "PAID"
      }
    }
  ]
  ```

---

### 4. Patch Payment Status
Updates only the status of an existing payment transaction. 
*Note: If the updated status is patched to `SUCCESS`, the associated bill's status is automatically updated to `PAID`.*

* **Method**: `PATCH`
* **Path**: `/api/payments/{id}/status`
* **Query Parameters**:
  * `status` (Required, String): New status (e.g. `SUCCESS`, `FAILED`).
* **Success Response**: `200 OK`
  ```json
  {
    "id": 100,
    "amount": 275.50,
    "method": "CREDIT_CARD",
    "status": "SUCCESS",
    "bill": {
      "id": 10,
      "amount": 275.50,
      "status": "PAID"
    }
  }
  ```
* **Error Response**: `404 Not Found`
