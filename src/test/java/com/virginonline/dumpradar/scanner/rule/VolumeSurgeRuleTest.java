package com.virginonline.dumpradar.scanner.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.config.props.Scanner15mProperties;
import com.virginonline.dumpradar.scanner.model.Candle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VolumeSurgeRuleTest {

  private final VolumeSurgeRule rule =
      new VolumeSurgeRule(new Scanner15mProperties(BigDecimal.ZERO, BigDecimal.ZERO, 8));

  @Test
  void eightTimesMedian_passes() {
    List<Candle> candles = blocks("100", 4);
    candles.addAll(recent("800"));
    assertTrue(rule.isSurge(candles));
  }

  @Test
  void belowMultiple_fails() {
    List<Candle> candles = blocks("100", 4);
    candles.addAll(recent("799"));
    assertFalse(rule.isSurge(candles));
  }

  @Test
  void insufficientHistory_fails() {
    List<Candle> candles = blocks("100", 3); // 45 свечей < 75
    candles.addAll(recent("8000"));
    assertFalse(rule.isSurge(candles));
  }

  @Test
  void medianOfUnevenBlocks_notAverageOrMax() {
    List<Candle> candles = unevenBlocks();
    candles.addAll(recent("2000"));
    assertTrue(rule.isSurge(candles));

    List<Candle> below = unevenBlocks();
    below.addAll(recent("1999"));
    assertFalse(rule.isSurge(below));
  }

  // --- helpers ---

  private static List<Candle> blocks(String volume, int count) {
    List<Candle> candles = new ArrayList<>();
    for (int b = 0; b < count; b++) {
      for (int i = 0; i < 15; i++) {
        candles.add(candle(volume));
      }
    }
    return candles;
  }

  private static List<Candle> unevenBlocks() {
    List<Candle> candles = new ArrayList<>();
    for (String v : List.of("100", "200", "300", "500")) {
      for (int i = 0; i < 15; i++) {
        candles.add(candle(v));
      }
    }
    return candles;
  }

  private static List<Candle> recent(String volume) {
    List<Candle> candles = new ArrayList<>();
    for (int i = 0; i < 15; i++) {
      candles.add(candle(volume));
    }
    return candles;
  }

  private static Candle candle(String volume) {
    return new Candle(
        0L,
        new BigDecimal("100"),
        new BigDecimal("101"),
        new BigDecimal("99"),
        new BigDecimal("100"),
        new BigDecimal(volume));
  }
}
