package com.virginonline.dumpradar.scanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.config.props.Scanner15mProperties;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.model.VelocityHit;
import com.virginonline.dumpradar.testfix.MutableClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceVelocityTrackerTest {

  private static final Instant T0 = Instant.parse("2026-09-07T12:00:00Z");

  private final MutableClock clock = new MutableClock(T0);
  private final PriceVelocityTracker tracker =
      new PriceVelocityTracker(
          new Scanner15mProperties(new BigDecimal("0.25"), new BigDecimal("0.60"), 8));

  @Test
  void surge25in60m_singleHit() {
    tracker.update(List.of(ticker("PEPEUSDT", "100")), clock.instant()); // старт окна
    clock.advance(Duration.ofMinutes(61));

    List<VelocityHit> hits = tracker.update(List.of(ticker("PEPEUSDT", "126")), clock.instant());

    assertEquals(1, hits.size());
    assertEquals("PEPEUSDT", hits.getFirst().symbol());
    assertEquals(0, new BigDecimal("100").compareTo(hits.getFirst().startPrice()));
    assertEquals(0, new BigDecimal("126").compareTo(hits.getFirst().lastPrice()));
  }

  @Test
  void belowThreshold_empty() {
    tracker.update(List.of(ticker("PEPEUSDT", "100")), clock.instant());
    clock.advance(Duration.ofMinutes(61));

    List<VelocityHit> hits = tracker.update(List.of(ticker("PEPEUSDT", "124")), clock.instant());

    assertTrue(hits.isEmpty());
  }

  @Test
  void surge6h_uses6hWindowStart() {

    List<VelocityHit> hits = List.of();
    double price = 100.0;
    for (int i = 0; i <= 36; i++) {
      hits =
          tracker.update(
              List.of(ticker("PEPEUSDT", String.format(java.util.Locale.ROOT, "%.2f", price))),
              clock.instant());
      clock.advance(Duration.ofMinutes(10));
      price *= 1.0132;
    }

    assertEquals(1, hits.size());
    assertEquals(0, new BigDecimal("100").compareTo(hits.getFirst().startPrice()));
  }

  @Test
  void holdingLevel_noRepeatHit() {
    tracker.update(List.of(ticker("PEPEUSDT", "100")), clock.instant());
    clock.advance(Duration.ofMinutes(61));
    tracker.update(List.of(ticker("PEPEUSDT", "126")), clock.instant()); // hit #1
    clock.advance(Duration.ofSeconds(10));

    List<VelocityHit> second = tracker.update(List.of(ticker("PEPEUSDT", "130")), clock.instant());

    assertTrue(second.isEmpty());
  }

  @Test
  void windowNotAccumulated_empty() {
    List<VelocityHit> hits = tracker.update(List.of(ticker("PEPEUSDT", "100000")), clock.instant());

    assertTrue(hits.isEmpty());
  }

  @Test
  void loserForgotten_canHitAgain() {
    tracker.update(List.of(ticker("PEPEUSDT", "100")), clock.instant());
    clock.advance(Duration.ofMinutes(61));
    tracker.update(List.of(ticker("PEPEUSDT", "126")), clock.instant()); // hit #1
    tracker.update(List.of(), clock.instant());
    clock.advance(Duration.ofMinutes(61));

    List<VelocityHit> again = tracker.update(List.of(ticker("PEPEUSDT", "130")), clock.instant());

    assertTrue(again.isEmpty());
  }

  private static Ticker ticker(String symbol, String last) {
    return new Ticker(symbol, new BigDecimal(last), 0.0, new BigDecimal("5000000"), 0L);
  }
}
