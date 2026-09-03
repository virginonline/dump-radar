package com.virginonline.dumpradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Set;

@ConfigurationProperties(prefix = "app.prefilter")
public record PrefilterProperties(
        BigDecimal minVolume24h,
        BigDecimal minPrice,
        int minListingAgeDays,
        Set<String> deniedBaseCoins) { }
