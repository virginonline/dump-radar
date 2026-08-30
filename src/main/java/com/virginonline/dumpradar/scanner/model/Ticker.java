package com.virginonline.dumpradar.scanner.model;

import java.math.BigDecimal;

public record Ticker(String symbol,
                    BigDecimal last,
                    double change24h,
                    BigDecimal volumeQuote,
                    long timestamp) {
}
