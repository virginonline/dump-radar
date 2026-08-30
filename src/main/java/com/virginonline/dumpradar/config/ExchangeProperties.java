package com.virginonline.dumpradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record ExchangeProperties(Map<String, Endpoint> exchanges) {
    public record Endpoint(String baseUrl) {
    }
}
