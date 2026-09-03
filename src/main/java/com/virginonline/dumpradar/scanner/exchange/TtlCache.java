package com.virginonline.dumpradar.scanner.exchange;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public final class TtlCache<T> {
    private final Duration ttl;
    private final Clock clock;
    private final Supplier<T> loader;
    private volatile Cached<T> cached;

    public TtlCache(Duration ttl, Clock clock, Supplier<T> loader) {
        this.ttl = ttl;
        this.clock = clock;
        this.loader = loader;
    }

    public T get() {
        Cached<T> snapshot = cached;
        if (snapshot != null && snapshot.loadedAt().plus(ttl).isAfter(clock.instant())) {
            return snapshot.value();
        }
        synchronized (this) {
            snapshot = cached;
            if (snapshot != null && snapshot.loadedAt().plus(ttl).isAfter(clock.instant())) {
                return snapshot.value();
            }
            T value = loader.get();
            cached = new Cached<>(value, clock.instant());
            return value;
        }
    }

    private record Cached<T>(T value, Instant loadedAt) {
    }

}
