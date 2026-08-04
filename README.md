# Online Billing System

Welcome to the **Online Billing System** repository! This is a modern, microservice-based architecture designed to manage customers, generate invoices, track payments, and provide a polished user interface for billing operations.

---

## 🏗️ Architecture Overview

This project is built using a microservices pattern with two primary backend services:

### 1. Billing Service (Port `8081`)
- **Role:** The orchestration and business logic layer.
- **Responsibilities:**
  - Exposes REST APIs for the frontend dashboard.
  - Validates all incoming data (e.g., proper email formats, phone numbers).
  - Handles the business logic for calculating partial payments, applying taxes, and updating bill statuses (PENDING, PARTIALLY_PAID, PAID).
  - Serves the static HTML/CSS/JS frontend dashboard.
- **Rules:** Never talks directly to the database. All persistence must go through the Data Pool Service.

### 2. Data Pool Service (Port `8082`)
- **Role:** The persistence and data management layer.
- **Responsibilities:**
  - Manages the database schema using Hibernate/JPA.
  - Connects to the local MySQL instance (`billing_db`).
  - Handles automatic timestamping (e.g., `created_at`, `paid_at`, `payment_date`).
  - Enforces entity-level constraints and manages the lifecycle of `Customer`, `Bill`, and `Payment` entities.

---

## ✨ Key Features

- **Customer Management:** Create and edit customer profiles with strict data validation.
- **Invoice Generation:** Generate custom bills that automatically calculate subtotal, tax (18%), and totals.
- **Payment Processing:** Record partial or full payments. The system dynamically calculates outstanding balances and updates the invoice state machine in real-time.
- **Client-Side PDF Generation:** Users can preview invoices and instantly download pixel-perfect PDF copies directly from their browser, powered by `html2pdf.js` (saving expensive server resources).
- **Interactive Dashboard:** A beautiful, responsive, and dynamic UI built with Vanilla JavaScript, HTML, and custom CSS.

---

## 🛠️ Technology Stack

- **Backend:** Java 17, Spring Boot, Spring Web, Spring Data JPA, Jakarta Validation
- **Database:** MySQL
- **Frontend:** HTML5, CSS3, Vanilla JavaScript, html2pdf.js
- **Build Tool:** Maven

---

## 🚀 Getting Started

Follow these instructions to get the project up and running on your local machine.

### Prerequisites
1. **Java 17** installed and configured.
2. **Maven** installed (or use the provided `./mvnw` wrapper).
3. **MySQL** installed and running on `localhost:3306`.

### Step 1: Database Setup
Make sure you have a local MySQL server running. The Data Pool service expects the following credentials by default:
- **Database:** `billing_db` (It will automatically create it if it doesn't exist)
- **Username:** `root`
- **Password:** `V@ibhav585`

*(Note: If your local MySQL password is different, please update `datapool-service/src/main/resources/application.properties` before running).*

### Step 2: Start the Data Pool Service
Open a terminal in the root directory of the project and run:
```bash
./mvnw -pl datapool-service spring-boot:run
```
Wait for the service to start fully on port `8082`.

### Step 3: Start the Billing Service
Open a new terminal window in the root directory and run:
```bash
./mvnw -pl billing-service spring-boot:run
```
Wait for the service to start fully on port `8081`.

---

## 🖥️ Usage

Once both services are running successfully:

1. **Open the Dashboard:** 
   Navigate your web browser to [http://localhost:8081/dashboard.html](http://localhost:8081/dashboard.html)
2. **Explore:**
   - Go to the **Customers** tab to register a new client.
   - Go to the **Invoices & Bills** tab to generate a new invoice.
   - Click **View Invoice** on any generated bill to preview it and click **Download PDF**.
   - Go to the **Record Payment** tab to settle an outstanding balance.

---

## 📂 Documentation

Additional engineering notes, API contracts, and historical design decisions are stored in the `/docs` directory at the root of this repository.

---
*Developed by the Online Billing System Team*
