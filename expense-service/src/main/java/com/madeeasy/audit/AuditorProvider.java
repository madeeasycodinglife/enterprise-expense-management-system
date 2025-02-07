package com.madeeasy.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AuditorProvider implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("User : {}", authentication);
        log.info("User Principle : {}", authentication.getPrincipal());
        log.info("User Name : {}", authentication.getName());

        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return Optional.of("SELF-REGISTERED"); // Default user if authentication is not available
        }
        log.info("User : {}", authentication);
        log.info("User Principle : {}", authentication.getPrincipal());
        log.info("User Name : {}", authentication.getName());
        return Optional.of(authentication.getName()); // Returns the username of the logged-in user
    }
}

