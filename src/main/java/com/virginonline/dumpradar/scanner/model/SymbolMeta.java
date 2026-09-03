package com.virginonline.dumpradar.scanner.model;

public record SymbolMeta(String symbol,
                         String baseCoin,
                         long firstSeen,
                         String symbolStatus
) {
}
