package com.virginonline.dumpradar.scanner.model;

import java.math.BigDecimal;

public record Candle(long openTime,
                     BigDecimal open,
                     BigDecimal high,
                     BigDecimal low,
                     BigDecimal close,
                     BigDecimal volumeQuote) {
}
