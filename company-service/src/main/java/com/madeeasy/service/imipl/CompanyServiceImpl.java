package com.madeeasy.service.imipl;

import com.madeeasy.dto.request.CompanyRequestDTO;
import com.madeeasy.dto.response.CompanyResponseDTO;
import com.madeeasy.entity.Company;
import com.madeeasy.repository.CompanyRepository;
import com.madeeasy.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        Company savedCompany = this.companyRepository.save(company);
        return CompanyResponseDTO.builder()
                .name(savedCompany.getName())
                .domain(savedCompany.getDomain())
                .build();
    }
}
