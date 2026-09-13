package com.virginonline.dumpradar.config.props;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scanner15m")
public record Scanner15mProperties(BigDecimal surge60m, BigDecimal surge6h, int volumeMultiple) {}
