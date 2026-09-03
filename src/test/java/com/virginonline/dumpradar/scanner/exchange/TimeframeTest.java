package com.virginonline.dumpradar.scanner.exchange;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeframeTest {

    @Test
    void codes_matchBitgetCanonicalCasing() {
        assertEquals("1m", Timeframe.M1.code());
        assertEquals("5m", Timeframe.M5.code());
        assertEquals("15m", Timeframe.M15.code());
        assertEquals("1H", Timeframe.H1.code());
        assertEquals("4H", Timeframe.H4.code());
        assertEquals("1D", Timeframe.D1.code());
    }

    @Test
    void durations_matchTimeframes() {
        assertEquals(Duration.ofMinutes(1), Timeframe.M1.duration());
        assertEquals(Duration.ofMinutes(5), Timeframe.M5.duration());
        assertEquals(Duration.ofMinutes(15), Timeframe.M15.duration());
        assertEquals(Duration.ofHours(1), Timeframe.H1.duration());
        assertEquals(Duration.ofHours(4), Timeframe.H4.duration());
        assertEquals(Duration.ofDays(1), Timeframe.D1.duration());
    }
}
