package com.virginonline.dumpradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.screener")
public record ScreenProperties(
        BigDecimal minBodyPct,
        BigDecimal volumeMultiple,
        BigDecimal maxCloseFromHigh,
        int historyBars
) {
}