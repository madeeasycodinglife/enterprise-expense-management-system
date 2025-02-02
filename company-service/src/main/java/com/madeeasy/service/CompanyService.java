package com.madeeasy.service;

import com.madeeasy.dto.request.CompanyRequestDTO;
import com.madeeasy.dto.response.CompanyResponseDTO;

public interface CompanyService {
    CompanyResponseDTO registerCompany(CompanyRequestDTO companyRequestDTO);

    CompanyResponseDTO getCompanyByDomainName(String domain);
}
