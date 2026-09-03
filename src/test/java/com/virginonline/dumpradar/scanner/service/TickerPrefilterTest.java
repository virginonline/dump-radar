package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.PrefilterProperties;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TickerPrefilterTest {

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");

    private final TickerPrefilter prefilter = new TickerPrefilter();
    private final PrefilterProperties props = new PrefilterProperties(
            new BigDecimal("2000000"), new BigDecimal("0.001"), 14,
            Set.of("USDC", "WBTC", "WETH"));

    @Test
    void allRulesPass_keeps() {
        Map<String, SymbolMeta> symbols = Map.of("BTCUSDT", old("BTCUSDT", "BTC"));

        List<Ticker> out = prefilter.filter(
                List.of(ticker("BTCUSDT", "78000", "793000000")), symbols, props, NOW);

        assertEquals(1, out.size());
        assertEquals("BTCUSDT", out.getFirst().symbol());
    }

    @Test
    void volumeBoundary() {
        Map<String, SymbolMeta> symbols = Map.of(
                "EXACTUSDT", old("EXACTUSDT", "EXACT"),
                "UNDERUSDT", old("UNDERUSDT", "UNDER"));

        List<Ticker> out = prefilter.filter(List.of(
                ticker("EXACTUSDT", "1", "2000000"),
                ticker("UNDERUSDT", "1", "1999999")), symbols, props, NOW);

        assertEquals(List.of("EXACTUSDT"), out.stream().map(Ticker::symbol).toList());
    }

    @Test
    void priceBoundary() {
        Map<String, SymbolMeta> symbols = Map.of(
                "EXACTUSDT", old("EXACTUSDT", "EXACT"),
                "DUSTUSDT", old("DUSTUSDT", "DUST"));

        List<Ticker> out = prefilter.filter(List.of(
                ticker("EXACTUSDT", "0.001", "5000000"),
                ticker("DUSTUSDT", "0.0009", "5000000")), symbols, props, NOW);

        assertEquals(List.of("EXACTUSDT"), out.stream().map(Ticker::symbol).toList());
    }

    @Test
    void missingMeta_dropped() {
        List<Ticker> out = prefilter.filter(
                List.of(ticker("GHOSTUSDT", "1", "5000000")), Map.of(), props, NOW);

        assertEquals(0, out.size());
    }

    @Test
    void notNormalStatus_dropped() {
        Map<String, SymbolMeta> symbols = Map.of("MAINTUSDT",
                new SymbolMeta("MAINTUSDT", "MAINT", 0L, "maintain"));

        List<Ticker> out = prefilter.filter(
                List.of(ticker("MAINTUSDT", "1", "5000000")), symbols, props, NOW);

        assertEquals(0, out.size());
    }

    @Test
    void freshListing_ageWindow() {
        Map<String, SymbolMeta> symbols = Map.of(
                "TENUSDT", new SymbolMeta("TENUSDT", "TEN",
                        NOW.minus(Duration.ofDays(10)).toEpochMilli(), "normal"),
                "FIFTEENUSDT", new SymbolMeta("FIFTEENUSDT", "FIFTEEN",
                        NOW.minus(Duration.ofDays(15)).toEpochMilli(), "normal"));

        List<Ticker> out = prefilter.filter(List.of(
                ticker("TENUSDT", "1", "5000000"),
                ticker("FIFTEENUSDT", "1", "5000000")), symbols, props, NOW);

        assertEquals(List.of("FIFTEENUSDT"), out.stream().map(Ticker::symbol).toList());
    }

    @Test
    void deniedBaseCoin_dropped() {
        Map<String, SymbolMeta> symbols = Map.of("USDCUSDT", old("USDCUSDT", "USDC"));

        List<Ticker> out = prefilter.filter(
                List.of(ticker("USDCUSDT", "1", "5000000")), symbols, props, NOW);

        assertEquals(0, out.size());
    }

    @Test
    void substringIsNotDeny_hyundaiKept() {
        Map<String, SymbolMeta> symbols = Map.of("HYUNDAIUSDT", old("HYUNDAIUSDT", "HYUNDAI"));

        List<Ticker> out = prefilter.filter(
                List.of(ticker("HYUNDAIUSDT", "1", "5000000")), symbols, props, NOW);

        assertEquals(1, out.size());
    }

    private static SymbolMeta old(String symbol, String baseCoin) {
        return new SymbolMeta(symbol, baseCoin, 0L, "normal");
    }

    private static Ticker ticker(String symbol, String last, String volume) {
        return new Ticker(symbol, new BigDecimal(last), 0.1, new BigDecimal(volume), 0L);
    }
}
