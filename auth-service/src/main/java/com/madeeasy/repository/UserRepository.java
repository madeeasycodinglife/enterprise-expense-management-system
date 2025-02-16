package com.madeeasy.repository;

import com.madeeasy.entity.Role;
import com.madeeasy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByCompanyDomainAndRole(String companyDomain, Role role);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByCompanyDomain(String companyDomain);

    // Custom query to find a user by companyDomain and role
    @Query("SELECT u FROM User u WHERE u.companyDomain = :companyDomain AND u.role = :role AND u.email = :email")
    Optional<User> findByCompanyDomainAndAdminRole(String companyDomain, Role role, String email);
}
