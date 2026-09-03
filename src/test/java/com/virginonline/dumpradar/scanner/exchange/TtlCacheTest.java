package com.virginonline.dumpradar.scanner.exchange;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TtlCacheTest {

    @Test
    void loadsOnceWithinTtl() {
        AtomicInteger loads = new AtomicInteger();
        MutableClock clock = new MutableClock(Instant.parse("2026-09-04T12:00:00Z"));
        TtlCache<String> cache = new TtlCache<>(Duration.ofHours(1), clock,
                () -> "v" + loads.incrementAndGet());

        assertEquals("v1", cache.get());
        assertEquals("v1", cache.get());

        assertEquals(1, loads.get());
    }

    @Test
    void reloadsAfterTtlExpiry() {
        AtomicInteger loads = new AtomicInteger();
        MutableClock clock = new MutableClock(Instant.parse("2026-09-04T12:00:00Z"));
        TtlCache<String> cache = new TtlCache<>(Duration.ofHours(1), clock,
                () -> "v" + loads.incrementAndGet());

        assertEquals("v1", cache.get());
        clock.advance(Duration.ofHours(1).plusSeconds(1));

        assertEquals("v2", cache.get());
        assertEquals(2, loads.get());
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
