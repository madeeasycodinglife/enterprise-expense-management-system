package com.madeeasy.controller;


import com.madeeasy.dto.request.CompanyRequestDTO;
import com.madeeasy.dto.response.CompanyResponseDTO;
import com.madeeasy.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/company-service")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping(path = "/register")
    public ResponseEntity<?> registerCompany(@RequestBody CompanyRequestDTO request) {
        CompanyResponseDTO company = companyService.registerCompany(request);
        return ResponseEntity.ok(company);
    }

    @GetMapping(path = "/domain-name/{domain}")
    public ResponseEntity<?> getCompany(@PathVariable String domain) {
        CompanyResponseDTO companyByDomainName = this.companyService.getCompanyByDomainName(domain);
        return ResponseEntity.ok(companyByDomainName);
    }
}
