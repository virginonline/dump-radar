package com.virginonline.dumpradar.scanner.model;

import java.math.BigDecimal;

public record VelocityHit(String symbol, BigDecimal startPrice, BigDecimal lastPrice) {}
