package com.madeeasy;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@OpenAPIDefinition(
        info = @Info(
                title = "Approval Service API",
                version = "1.0",
                description = "API for handling approval requests for expenses",
                contact = @Contact(
                        email = "madeeasycodinglife@gmail.com",
                        name = "Pabitra Bera",
                        url = "https://www.linkedin.com/in/pabitra-bera"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://demo.org/licenses/LICENSE-2.0"
                ),
                termsOfService = "Terms and Conditions for accessing the Approval API.",
                summary = "This API provides endpoints for submitting expense approval requests."
        )
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        in = SecuritySchemeIn.HEADER,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Bearer with JWT authentication"
)
@SpringBootApplication
@EnableDiscoveryClient
public class ApprovalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApprovalServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    // Non-load-balanced RestTemplate for specific host calls (e.g., localhost)
    @Bean
    public RestTemplate localRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}