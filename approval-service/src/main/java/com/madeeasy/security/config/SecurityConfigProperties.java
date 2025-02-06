package com.madeeasy.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "security.authorization")
public class SecurityConfigProperties {
    private List<PathConfig> paths;

    @Data
    public static class PathConfig {
        private String path;
        private String method;
        private List<String> roles;
    }
}
