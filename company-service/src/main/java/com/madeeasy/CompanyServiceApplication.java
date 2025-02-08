package com.madeeasy;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@OpenAPIDefinition(
        info = @Info(
                title = "Company Service API",
                version = "1.0",
                description = "API for managing company registrations and retrieving company information by domain name.",
                contact = @Contact(
                        email = "madeeasycodinglife@gmail.com",
                        name = "Pabitra Bera",
                        url = "https://www.linkedin.com/in/pabitra-bera"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://demo.org/licenses/LICENSE-2.0"
                ),
                termsOfService = "Terms and Conditions for accessing the Company API.",
                summary = "This API provides endpoints for registering companies and retrieving company details by their domain name."
        )
)
@SpringBootApplication
@EnableDiscoveryClient
public class CompanyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompanyServiceApplication.class, args);
    }
}