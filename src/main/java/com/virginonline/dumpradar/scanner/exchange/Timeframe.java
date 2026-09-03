package com.virginonline.dumpradar.scanner.exchange;

import java.time.Duration;

public enum Timeframe {

    M1("1m", Duration.ofMinutes(1)),
    M5("5m", Duration.ofMinutes(5)),
    M15("15m", Duration.ofMinutes(15)),
    H1("1H", Duration.ofHours(1)),
    H4("4H", Duration.ofHours(4)),
    D1("1D", Duration.ofDays(1));

    private final String code;
    private final Duration duration;

    Timeframe(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String code() {
        return code;
    }

    public Duration duration() {
        return this.duration;
    }
}
