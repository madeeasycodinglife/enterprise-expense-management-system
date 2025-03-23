# 🌐 API Gateway - Enterprise Expense Management System

A **Spring Cloud Gateway** service that acts as a **reverse proxy**, routing requests to appropriate microservices and handling cross-cutting concerns like **authentication, rate limiting, circuit breaking, and global CORS configuration**.

## 🚀 Features
✅ **Spring Cloud Gateway** for intelligent routing  
✅ **Resilience4j Circuit Breaker** for fault tolerance  
✅ **Global CORS Configuration**  
✅ **Eureka Discovery Client** for service registration & discovery  
✅ **Spring Boot Actuator** for monitoring & tracing  
✅ **Swagger API Documentation**

---

## 📂 Folder Structure
```
api-gateway/
│── src/main/java/com/madeeasy/
│── src/main/resources/
│   ├── application.yml
│── pom.xml
│── README.md
```

---

## 🔧 Setup & Configuration

### 1️⃣ Run the Gateway
```sh
mvn spring-boot:run
```

### 2️⃣ Access Swagger API Docs
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Docs:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 3️⃣ Eureka Service Discovery
- **Eureka Dashboard:** [http://localhost:8761/](http://localhost:8761/)

---

## 📡 API Gateway Routes

### 🔹 **Approval Service**
```http
http://localhost:8080/approval-service/**
```
🔹 **Fallback URL:** `/fallback/approval-service`

---

### 🔹 **Company Service**
```http
http://localhost:8080/company-service/**
```
🔹 **Fallback URL:** `/fallback/company-service`

---

### 🔹 **Auth Service**
```http
http://localhost:8080/auth-service/**
```
🔹 **Fallback URL:** `/fallback/auth-service`

---

### 🔹 **Expense Service**
```http
http://localhost:8080/expense-service/**
```
🔹 **Fallback URL:** `/fallback/expense-service`

---

### 🔹 **Notification Service**
```http
http://localhost:8080/notification-service/**
```
🔹 **Fallback URL:** `/fallback/notification-service`

---

## ⚡ Circuit Breaker (Resilience4j)
The **circuit breaker** protects downstream services from cascading failures.

### 🔹 Configuration
- **Failure Rate Threshold:** `50%`
- **Minimum Calls Before Breaker Triggers:** `5`
- **Sliding Window Size:** `10`
- **Timeout Duration:** `50s`
- **Auto Transition to Half-Open:** ✅

---

## 🔄 Eureka Service Discovery
The API Gateway is registered with **Eureka** for automatic service discovery.

- **Eureka Server URL:** `http://localhost:8761/eureka/`
- **Service Discovery Enabled:** ✅
- **Auto Fetch Interval:** `30s`

---

## 🔍 Monitoring & Tracing

### 🔹 Actuator Endpoints
- **Health Check:**
  ```http
  GET http://localhost:8080/actuator/health
  ```
- **Circuit Breaker Status:**
  ```http
  GET http://localhost:8080/actuator/health/circuitbreakers
  ```
- **Tracing Enabled:** ✅ (Zipkin)

🔹 **Zipkin Dashboard:** [http://localhost:9411/](http://localhost:9411/)

---

## 📬 Contributing
Feel free to fork, create a branch, and submit a PR! 🚀
