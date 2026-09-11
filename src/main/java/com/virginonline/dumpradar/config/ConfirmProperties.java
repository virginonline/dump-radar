package com.virginonline.dumpradar.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.confirmation")
public record ConfirmProperties(BigDecimal nearHigh, BigDecimal gapDrop) {}
