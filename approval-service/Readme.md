# Approval Service - Enterprise Expense System

## Overview
The Approval Service is a microservice responsible for handling expense approval requests within the **Enterprise Expense System**. It enables employees to submit expense approvals while allowing managers, finance teams, and administrators to review, approve, or reject expenses.

## Features
- **Submit Expense Approvals**
- **Approval & Rejection Workflows**
- **Role-Based Access Control (RBAC)**
- **Resilience with Circuit Breaker & Retry Mechanisms**
- **Eureka Service Discovery**
- **Tracing with Zipkin**
- **H2 Database for Development**
- **API Documentation with SpringDoc**

## Technologies Used
- **Spring Boot** (Microservices Framework)
- **Spring Security** (Authentication & Authorization)
- **Spring Cloud Eureka** (Service Discovery)
- **Resilience4j** (Circuit Breaker & Retry)
- **H2 Database** (In-memory DB for Dev)
- **Spring Actuator** (Monitoring & Health Checks)
- **SpringDoc & Swagger UI** (API Documentation)
- **Thymeleaf** (Approval UI Rendering)

## API Endpoints

### Submit Expense Approval
**POST:**  `http://localhost:8080/approval-service/ask-for-approve`

**Headers:**  
`Authorization: Bearer <JWT_TOKEN>`  
Example Token:
```
eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI4MmQ0ZDBmOS01MTlhLTRkMGMtOWNjYi05MzZjNWE2ZDBmYTEiLCJzdWIiOiJhYmNAZ21haWwuY29tIiwicm9sZXMiOiJFTVBMT1lFRSIsImNvbXBhbnlEb21haW4iOiJleGFtcGxlLmNvbSIsImlzcyI6Im1hZGVlYXN5Y29kaW5nbGlmZSIsImlhdCI6MTczODg5ODk3NiwiZXhwIjoxNzM5MTU4MTc2fQ.Byzk_HBaGeEVY39R4nNDYGzd8OKEo-tO2VL-WNbzA3Ffj46CdGUH5MmQPEWKT5y6
```

**Request Body:**
```json
{ 
    "expenseId": 12345,
    "companyDomain": "example.com",
    "title": "Business Trip to NY",
    "description": "Flight and accommodation for the business trip to New York.",
    "amount": 1500.00,
    "category": "TRAVEL",
    "expenseDate": "2025-01-30T10:30:00"
}
```

## UI (Thymeleaf Template)
The service includes a **Thymeleaf-based UI** under `resources/templates` for displaying approval statuses with Bootstrap styling.
- **Success, Rejection, and Already Responded UI Pages**
- **Dynamic Expense Information Display**
- **Auto-Close Button for Pop-ups**

## Database Configuration
- **H2 Database** is used for in-memory storage in development mode.
- The H2 Console is enabled at: `http://localhost:8085/h2-console`

![approval-h2-db](../images/approval-h2-db.png)

## How to Run
1. Clone the repository
2. Configure environment variables if needed
3. Run the application:
   ```sh
   mvn spring-boot:run
   ```
4. Open Swagger UI at: `http://localhost:8080/swagger-ui/index.html?urls.primaryName=approval-service`

![approval-swagger](../images/approval-swagger.png)

## More Endpoints
To explore all available endpoints, refer to the project documentation or check the API definition in **SpringDoc**.

## 📬 Contributing
Feel free to fork the repo, create a branch, and submit a PR! 🚀
