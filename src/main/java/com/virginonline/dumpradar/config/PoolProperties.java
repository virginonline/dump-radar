package com.virginonline.dumpradar.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pool")
public record PoolProperties(Duration watching4h, Duration watching15m, Duration cooldown) {}
