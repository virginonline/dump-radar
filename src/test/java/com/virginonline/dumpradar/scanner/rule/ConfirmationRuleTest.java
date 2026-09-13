package com.virginonline.dumpradar.scanner.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.config.props.ConfirmProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Confirmation;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import com.virginonline.dumpradar.scanner.model.Source;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfirmationRuleTest {

  private static final Instant NOW = Instant.parse("2026-09-07T12:30:00Z");
  private static final BigDecimal ANCHOR = new BigDecimal("100");

  private final ConfirmationRule rule =
      new CompositeConfirmationRule(
          List.of(new RedCandleWithVolumePattern(), new WickRejectionPattern()),
          new ConfirmProperties(new BigDecimal("0.04"), new BigDecimal("0.28")));

  @Test
  void redCandleWithVolume_nearAnchor_confirmed() {
    List<Candle> candles = history();
    candles.add(candle("100", "99", "95", "95.5", "300"));

    Optional<Confirmation> confirmation = rule.check(candidate(), candles, NOW);

    assertTrue(confirmation.isPresent());
    assertEquals(ConfirmationKind.RED_CANDLE_WITH_VOLUME, confirmation.get().kind());
    assertEquals(NOW, confirmation.get().at());
  }

  @Test
  void redCandleWithVolume_belowZone_empty() {
    List<Candle> candles = history();
    candles.add(candle("93", "90", "89", "89.5", "300")); // high 90 < 96

    assertTrue(rule.check(candidate(), candles, NOW).isEmpty());
  }

  @Test
  void wickRejection_nearAnchor_confirmed() {
    List<Candle> candles = history();
    // open=close=97, high=100, low=96: wick = (100-97)/(100-96) = 0.75 > 0.60
    candles.add(candle("97", "100", "96", "97", "50"));

    Optional<Confirmation> confirmation = rule.check(candidate(), candles, NOW);

    assertTrue(confirmation.isPresent());
    assertEquals(ConfirmationKind.WICK_REJECTION, confirmation.get().kind());
  }

  @Test
  void wickRejection_belowZone_empty() {
    List<Candle> candles = history();
    candles.add(candle("87", "90", "86", "87", "50"));

    assertTrue(rule.check(candidate(), candles, NOW).isEmpty());
  }

  @Test
  void volumeBelowAverage_empty() {
    List<Candle> candles = history();
    candles.add(candle("98.5", "99", "98", "98.4", "90"));

    assertTrue(rule.check(candidate(), candles, NOW).isEmpty());
  }

  @Test
  void greenCandleWithVolume_nearAnchor_empty() {
    List<Candle> candles = history();
    candles.add(candle("96", "99", "95.5", "99", "300"));

    assertTrue(rule.check(candidate(), candles, NOW).isEmpty());
  }

  @Test
  void flatCandle_noCrash_noConfirm() {
    List<Candle> candles = history();
    candles.add(candle("97", "97", "97", "97", "500")); // high == low

    assertTrue(rule.check(candidate(), candles, NOW).isEmpty());
  }

  @Test
  void firstConfirmingMomentWins_candleOuterIteration() {
    List<Candle> candles = history();
    candles.add(candle("97", "100", "96", "97", "50"));
    candles.add(candle("100", "100", "95", "95.5", "300"));

    Optional<Confirmation> confirmation = rule.check(candidate(), candles, NOW);

    assertTrue(confirmation.isPresent());
    assertEquals(ConfirmationKind.WICK_REJECTION, confirmation.get().kind());
  }

  private static List<Candle> history() {
    List<Candle> candles = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      candles.add(candle("80", "80", "79", "80", "100"));
    }
    return candles;
  }

  private static Candle candle(String open, String high, String low, String close, String volume) {
    return new Candle(
        0L,
        new BigDecimal(open),
        new BigDecimal(high),
        new BigDecimal(low),
        new BigDecimal(close),
        new BigDecimal(volume));
  }

  private static Candidate candidate() {
    return new Candidate(
        "PEPEUSDT-BITGET-20260907T120030Z",
        "PEPE",
        "PEPEUSDT",
        Set.of(Exchange.BITGET),
        Source.SCAN_4H,
        CandidateState.WATCHING,
        ANCHOR,
        new BigDecimal("70"),
        NOW.minus(Duration.ofMinutes(30)),
        NOW.plus(Duration.ofHours(23)),
        null,
        NOW.minus(Duration.ofMinutes(30)));
  }
}
