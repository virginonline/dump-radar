package com.virginonline.dumpradar.config;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record ExchangeProperties(Cache cache, Map<Exchange, Endpoint> exchanges) {
    public record Cache(Duration ttl) { }

    public record Endpoint(String baseUrl) {
    }
}
