# Enterprise Expense Management System

![Enterprise Expense Management System](file:///C:/internships/zidio/project-images/frontend-home-page.png)

A **Spring Boot Microservices-based** system for managing enterprise expenses with **JWT authentication** and **global Swagger documentation**.

## 🚀 Features
- **Microservices Architecture** with Spring Boot
- **Secure Authentication** using JWT
- **API Gateway** for unified access
- **Expense Approval Workflow**
- **Email Notifications**
- **Global API Documentation** with Swagger
- **Service Discovery** using Eureka

## 🏗️ Tech Stack
- **Backend:** Spring Boot, Spring Security, JPA, H2 Database
- **API Gateway:** Spring Cloud Gateway
- **Service Discovery:** Eureka Server
- **Authentication:** JWT
- **Communication:** REST
- **Deployment:** Docker, AWS [Not Included]

## 📂 Folder Structure
```
enterprise-expense-management-system/
│── api-gateway/
│── approval-service/
│── auth-service/
│── company-service/
│── eureka-server/
│── expense-service/
│── notification-service/
│── .gitignore
│── mvnw 
│── pom.xml
```

## 🔧 Setup Instructions
1️⃣ **Clone the repository**
```sh
git clone https://github.com/madeeasycodinglife/enterprise-expense-management-system.git  
cd enterprise-expense-management-system  
```
2️⃣ **Run the services**
- Start **Eureka Server** and **Zipkin** first
- Then, start the other microservices
- Use `mvn spring-boot:run` for each service  

3️⃣ **Access Swagger Docs**
```sh
http://localhost:8080/swagger-ui.html
```

4️⃣ **Zipkin UI** for tracing
```sh
http://localhost:9411
```

## ⚠️ Important Notes
- **Simplified Design**: The application does not manage complex relationships like **Many-to-Many (MTM)**. For instance, a user can only register and manage one company and its employees. Developers interested in supporting multiple companies and associations can fork and extend the functionality as needed. This will be added in a future private project.

- **Expense and Currency Handling**: Expenses are stored as simple numbers (e.g., "100") without any currency attached. However, the email notifications send the amount as `$100`. **Payment gateway** integration and **AI features** are not included and will be addressed in a future private project.

## 📬 Contributing
Feel free to fork the repo, create a branch, and submit a PR! 🚀
