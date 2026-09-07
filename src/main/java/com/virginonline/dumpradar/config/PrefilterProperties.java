package com.virginonline.dumpradar.config;

import java.math.BigDecimal;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.prefilter")
public record PrefilterProperties(
    BigDecimal minVolume24h,
    BigDecimal minPrice,
    int minListingAgeDays,
    Set<String> deniedBaseCoins) {}
