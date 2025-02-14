package com.madeeasy.controller;

import com.madeeasy.dto.request.CompanyRequestDTO;
import com.madeeasy.dto.response.CompanyResponseDTO;
import com.madeeasy.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/company-service")
@SecurityRequirement(name = "Bearer Authentication") // Global security for all endpoints
@Tag(
        name = "Company Management",
        description = "API for managing company data, including registering a new company and retrieving company details by domain."
)
@Validated
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "Register a new company",
            description = "Registers a new company with the provided details and returns the created company information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company registered successfully",
                    content = @Content(schema = @Schema(implementation = CompanyResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "409", description = "Company with the given domain already exists", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(path = "/register")
    public ResponseEntity<CompanyResponseDTO> registerCompany(
           @Valid @RequestBody CompanyRequestDTO request) {
        CompanyResponseDTO company = companyService.registerCompany(request);
        return ResponseEntity.ok(company);
    }

    @Operation(
            summary = "Retrieve company details by domain",
            description = "Fetches company details using the domain name."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CompanyResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Company not found for the given domain", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(path = "/domain-name/{domain}")
    public ResponseEntity<CompanyResponseDTO> getCompany(
            @Parameter(description = "Company domain name", example = "example.com")
            @PathVariable String domain) {
        CompanyResponseDTO companyByDomainName = this.companyService.getCompanyByDomainName(domain);
        return ResponseEntity.ok(companyByDomainName);
    }
}
