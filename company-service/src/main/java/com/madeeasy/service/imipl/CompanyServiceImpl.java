package com.madeeasy.service.imipl;

import com.madeeasy.dto.request.CompanyRequestDTO;
import com.madeeasy.dto.response.CompanyResponseDTO;
import com.madeeasy.entity.Company;
import com.madeeasy.exception.ClientException;
import com.madeeasy.repository.CompanyRepository;
import com.madeeasy.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    public CompanyResponseDTO registerCompany(CompanyRequestDTO companyRequestDTO) {
        if (companyRepository.findByDomain(companyRequestDTO.getDomain()).isPresent()) {
            throw new RuntimeException("Company already exists");
        }
        Company company = new Company();
        company.setName(companyRequestDTO.getName());
        company.setDomain(companyRequestDTO.getDomain());
        company.setAutoApproveThreshold(companyRequestDTO.getAutoApproveThreshold());
        log.info("before saving : {}", company);
        Company savedCompany = this.companyRepository.save(company);
        log.info("after saving : {}", savedCompany);
        return CompanyResponseDTO.builder()
                .id(savedCompany.getId())
                .name(savedCompany.getName())
                .domain(savedCompany.getDomain())
                .autoApproveThreshold(savedCompany.getAutoApproveThreshold())
                .build();
    }

    @Override
    public CompanyResponseDTO getCompanyByDomainName(String domain) {
        Company foundCompany = this.companyRepository.findByDomain(domain).orElseThrow(() -> new ClientException("Invalid Company Domain or Company Domain Not Found !", HttpStatus.NOT_FOUND));

        return CompanyResponseDTO.builder()
                .id(foundCompany.getId())
                .name(foundCompany.getName())
                .domain(foundCompany.getDomain())
                .autoApproveThreshold(foundCompany.getAutoApproveThreshold())
                .build();
    }
}
