package com.madeeasy.repository;

import com.madeeasy.entity.Role;
import com.madeeasy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByCompanyDomainAndRole(String companyDomain, Role role);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
