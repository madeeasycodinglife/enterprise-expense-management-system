package com.madeeasy.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Hidden
@RestController
public class SpringCloudGatewayController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping(path = "/v3/api-docs/swagger-config")
    public Map<String, Object> v3swaggerConfigurations(ServerHttpRequest request) throws URISyntaxException, URISyntaxException {
        URI uri = request.getURI();
        URI url = new URI(uri.getScheme(), uri.getAuthority(), null, null, null);
        Map<String, Object> swaggerConfigMap = new LinkedHashMap<>();
        List<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrls = new LinkedList<>();
        discoveryClient.getServices().forEach(System.out::println);
        discoveryClient.getServices().stream().filter(serviceName -> serviceName.endsWith("-service")).forEach(serviceName -> {

            String serviceExactName = serviceName.replaceAll("-service", "");

            swaggerUrls.add(new AbstractSwaggerUiConfigProperties.SwaggerUrl(serviceName,
                    url + "/" + serviceExactName + "-service" + "/v3/api-docs", serviceName));
            System.out.println("service-exact-name : " + serviceExactName);
            System.out.println("after extracting swaggerUrl : " + swaggerUrls);
        });
        swaggerConfigMap.put("urls", swaggerUrls);
        return swaggerConfigMap;
    }
}