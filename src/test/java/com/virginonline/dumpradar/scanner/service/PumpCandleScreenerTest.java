package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.ScreenProperties;
import com.virginonline.dumpradar.scanner.model.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PumpCandleScreenerTest {
    private static Candle candle(double open, double high, double low, double close, double volume) {
        return new Candle(0L,
                BigDecimal.valueOf(open), BigDecimal.valueOf(high),
                BigDecimal.valueOf(low), BigDecimal.valueOf(close),
                BigDecimal.valueOf(volume));
    }

    private static PumpCandleScreener screener() {
        return new PumpCandleScreener(new ScreenProperties(
                new BigDecimal("0.30"), new BigDecimal("5"),
                new BigDecimal("0.25"), 20));
    }

    @Test
    void strongPumpWithVolumeSpike_passes() {
        List<Candle> candles = silentHistory();
        candles.add(candle(100, 140, 99, 134.9, 600));
        Optional<Candle> signal = screener().screen(candles);
        assertTrue(signal.isPresent());
    }

    @Test
    void insufficientHistory_rejected() {
        List<Candle> exactlyHistoryOnly = silentHistory();
        assertFalse(screener().screen(exactlyHistoryOnly).isPresent());
    }

    @Test
    void weakClose_rejected() {
        List<Candle> candles = silentHistory();
        candles.add(candle(100, 150, 95, 135, 600));
        Optional<Candle> signal = screener().screen(candles);
        assertFalse(signal.isPresent());
    }

    @Test
    void volumeBelowSpike_rejected() {
        List<Candle> candles = silentHistory();
        candles.add(candle(100, 140, 99, 134.9, 490));
        Optional<Candle> signal = screener().screen(candles);
        assertFalse(signal.isPresent());
    }

    @Test
    void exactly30pctAnd5x_passes() {
        List<Candle> candles = silentHistory();
        candles.add(candle(100, 135, 99, 130, 500));

        Optional<Candle> signal = screener().screen(candles);

        assertTrue(signal.isPresent());
    }

    @Test
    void bodyBelow30pct_rejected() {
        List<Candle> candles = silentHistory();
        candles.add(candle(100, 135, 99, 129.9, 600));
        assertFalse(screener().screen(candles).isPresent());
    }

    @Test
    void redCandle_rejected() {
        List<Candle> candles = silentHistory();
        candles.add(candle(100, 150, 90, 95, 600));
        assertFalse(screener().screen(candles).isPresent());
    }

    List<Candle> silentHistory() {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            candles.add(candle(100, 101, 99, 100, 100));
        }
        return candles;
    }
}