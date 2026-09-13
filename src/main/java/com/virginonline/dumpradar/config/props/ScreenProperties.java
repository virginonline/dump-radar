package com.virginonline.dumpradar.config.props;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.screener")
public record ScreenProperties(
    BigDecimal minBodyPct,
    BigDecimal volumeMultiple,
    BigDecimal maxCloseFromHigh,
    int historyBars) {}
