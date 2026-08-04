# Billing Service Project Memory

This file is the living project note for the Billing Service module. Update it as the project progresses.

## Current Scope

- Microservices online billing system with Billing Service and Data Pool Service.
- Billing Service owns orchestration, validation, invoice creation, payment processing, and customer summary APIs.
- Data Pool Service owns all persistence and database access.
- Billing Service port: `8081`.
- Data Pool Service port: `8080`.

## Important Paths

- API contract: `billing-service/docs/api-contract.md`
- Project breakdown: `billing-service/project_breakdown.md`
- First Billing Service slice: controller, DTOs, in-memory Data Pool client, and tests under `billing-service/src/main/java` and `billing-service/src/test/java`

## Working Rules

- Billing Service must not talk to the database directly.
- Use REST calls to Data Pool Service for customer, bill, and payment operations.
- Use `BigDecimal` for all money calculations.
- Keep request/response DTOs aligned with the API contract.
- Prefer mocked Data Pool responses while integrating early Billing Service work.

## Current Next Steps

- Replace the in-memory Data Pool client with a real REST client once the Data Pool Service contract is ready.
- Keep the API contract updated whenever endpoints, field names, or status values change.
- Expand tests around validation, partial payments, and error responses.
