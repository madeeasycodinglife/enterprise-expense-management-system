# 📬 Notification Service

## 🚀 Overview
The **Notification Service** is responsible for sending email notifications to users. It integrates with **SMTP (Gmail)** and is designed using **Spring Boot** with **Resilience4j Circuit Breaker**, **Eureka Discovery**, and **Zipkin Tracing**.

---

## ⚙️ Configuration

### 🔧 Server Configuration
```yaml
server:
  port: 8084
```

### 🛠️ Spring Boot Configuration
```yaml
spring:
  application:
    name: notification-service
```

### 📧 Email (SMTP) Configuration
```yaml
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### 🗄️ H2 Database Configuration
```yaml
  datasource:
    url: jdbc:h2:mem:notification-service
    username: sa
    password: sa
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 🏗️ Eureka Discovery Configuration
```yaml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### 🔄 Resilience4j Circuit Breaker Configuration
```yaml
resilience4j:
  circuitbreaker:
    configs:
      notificationServiceCircuitBreaker:
        failureRateThreshold: 20
        minimumNumberOfCalls: 5
        slidingWindowSize: 10
        waitDurationInOpenState: 5s
```

---

## 📌 API Endpoints

### 📩 Send Notification
🔹 **POST**: `http://localhost:8080/notification-service/`

**Headers:**
- `Authorization: Bearer <token>`

**Request Body:**
```json
{
    "expenseDetails": "expenseId=123&title=Business%20Dinner&description=Client%20meeting%20at%20restaurant&amount=150.00&category=Food&expenseDate=2025-02-07&accessToken=eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiJlMGRiZDNmYS03MWFmLTQ2ZGUtYTUyMC03ZjEzNzM0ODRlYTMiLCJzdWIiOiJhYmNAZ21haWwuY29tIiwicm9sZXMiOiJFTVBMT1lFRSIsImNvbXBhbnlEb21haW4iOiJleGFtcGxlLmNvbSIsImlzcyI6Im1hZGVlYXN5Y29kaW5nbGlmZSIsImlhdCI6MTczODg5NTE0NiwiZXhwIjoxNzM5MTU0MzQ2fQ.81BHZRLbpJeqPgV4dPCkjRqj-NaYdla4VHZniAuxhQ7Z-_j3s5D9j19X2ta8fpX2&emailId=paltubera2001@gmail.com",
    "approveLink": "http://localhost:8085/approval-service/approve?expenseId=123&title=Business%20Dinner&description=Client%20meeting%20at%20restaurant&amount=150.00&category=Food&expenseDate=2025-02-07&accessToken=yourAccessTokenHere&emailId=managerEmail@example.com&role=MANAGER",
    "rejectLink": "http://localhost:8085/approval-service/reject?expenseId=123&title=Business%20Dinner&description=Client%20meeting%20at%20restaurant&amount=150.00&category=Food&expenseDate=2025-02-07&accessToken=yourAccessTokenHere&emailId=managerEmail@example.com&role=MANAGER"
}
```

---

## 📊 Monitoring & Documentation

### 🔍 Swagger API Docs
- URL: [Swagger UI](http://localhost:8084/notification-service/v3/api-docs)

### 🗄️ H2 Database Console
- URL: [H2 Console](http://localhost:8084/h2-console)
- JDBC URL: `jdbc:h2:mem:notification-service`

![Notification-DB](../images/notification-h2-db.png)

Gmail : 

![Gmail](../images/email-notification-image.png)

### 📡 Zipkin Tracing
- URL: [Zipkin Dashboard](http://localhost:9411/)

---

## 🏁 Running the Service
1. Clone the repository.
2. Configure **SMTP settings** in `application.yml`.
3. Start the Eureka Server if not already running.
4. Run the Notification Service using:
   ```sh
   mvn spring-boot:run
   ```
5. Test the API using **Postman** or **Swagger UI**.

---
## 📜 More Endpoints

To explore all available endpoints, refer to the project documentation or check the API definition in **SpringDoc**.


## 📬 Contributing
Want to improve this service? Feel free to **fork** the repo, create a branch, and submit a PR! 🚀
