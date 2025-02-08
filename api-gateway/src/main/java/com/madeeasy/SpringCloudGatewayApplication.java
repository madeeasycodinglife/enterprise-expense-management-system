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
                title = "API GateWay",
                version = "1.0",
                description = "API for managing user authentication, registration, session management, and user details retrieval.",
                contact = @Contact(
                        email = "madeeasycodinglife@gmail.com",
                        name = "Pabitra Bera",
                        url = "https://www.linkedin.com/in/pabitra-bera"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://demo.org/licenses/LICENSE-2.0"
                ),
                termsOfService = "Terms and Conditions for accessing the Authentication API.",
                summary = "This API provides endpoints for user registration (sign-up), login (sign-in), token management (refresh and validation), and user profile management (update and retrieval)."
        )
)
@SpringBootApplication
@EnableDiscoveryClient
public class SpringCloudGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringCloudGatewayApplication.class, args);
    }
}