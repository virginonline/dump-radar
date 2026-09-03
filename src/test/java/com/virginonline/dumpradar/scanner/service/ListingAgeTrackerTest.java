package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListingAgeTrackerTest {

    private static final Instant T0 = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-09-04T00:00:00Z");

    @Test
    void bootstrap_marksExistingSymbolsAsOld() {
        ListingAgeTracker tracker = new ListingAgeTracker();

        Map<String, SymbolMeta> enriched = tracker.enrich(Map.of(
                "BTCUSDT", meta("BTCUSDT", "BTC"),
                "ETHUSDT", meta("ETHUSDT", "ETH")), T0);

        assertEquals(0L, enriched.get("BTCUSDT").firstSeen());
        assertEquals(0L, enriched.get("ETHUSDT").firstSeen());
    }

    @Test
    void newSymbolSeenAtItsFirstAppearance() {
        ListingAgeTracker tracker = new ListingAgeTracker();
        tracker.enrich(Map.of("BTCUSDT", meta("BTCUSDT", "BTC")), T0);

        Map<String, SymbolMeta> enriched = tracker.enrich(Map.of(
                "BTCUSDT", meta("BTCUSDT", "BTC"),
                "NEWUSDT", meta("NEWUSDT", "NEW")), T1);

        assertEquals(T1.toEpochMilli(), enriched.get("NEWUSDT").firstSeen());
        assertEquals(0L, enriched.get("BTCUSDT").firstSeen());
    }

    private static SymbolMeta meta(String symbol, String baseCoin) {
        return new SymbolMeta(symbol, baseCoin, 0L, "normal");
    }
}
