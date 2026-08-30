package com.virginonline.dumpradar.config;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record ExchangeProperties(Map<Exchange, Endpoint> exchanges) {
    public record Endpoint(String baseUrl) {
    }
}
