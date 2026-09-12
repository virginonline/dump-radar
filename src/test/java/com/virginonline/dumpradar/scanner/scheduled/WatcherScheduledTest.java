package com.virginonline.dumpradar.scanner.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.virginonline.dumpradar.config.ConfirmProperties;
import com.virginonline.dumpradar.config.PoolProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.PumpSignal;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.rule.CompositeConfirmationRule;
import com.virginonline.dumpradar.scanner.rule.RedCandleWithVolumePattern;
import com.virginonline.dumpradar.scanner.rule.WickRejectionPattern;
import com.virginonline.dumpradar.scanner.service.CandidatePool;
import com.virginonline.dumpradar.testfix.FakeRecorder;
import com.virginonline.dumpradar.testfix.MutableClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class WatcherScheduledTest {

  private static final Instant T0 = Instant.parse("2026-09-07T12:30:00Z");

  private final MutableClock clock = new MutableClock(T0);
  private final FakeRecorder recorder = new FakeRecorder();
  private final FakeClient client = new FakeClient();
  private final ConfirmProperties confirmProps =
      new ConfirmProperties(new BigDecimal("0.04"), new BigDecimal("0.28"));
  private final CandidatePool pool =
      new CandidatePool(
          recorder,
          clock,
          new PoolProperties(Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(4)),
          new ObjectMapper());
  private final WatcherScheduled watcher =
      new WatcherScheduled(
          pool,
          List.of(client),
          clock,
          new CompositeConfirmationRule(
              List.of(new RedCandleWithVolumePattern(), new WickRejectionPattern()), confirmProps),
          confirmProps);

  @Test
  void wickAtAnchor_confirms() {
    admit("100", "70");
    List<Candle> candles = history();
    candles.add(candle("97", "100", "96", "97", "50")); // wick 0.75
    client.candlesBySymbol.put("PEPEUSDT", candles);

    watcher.run();

    assertEquals(CandidateState.CONFIRMED, stored().state());
    assertEquals(T0, stored().confirmedAt());
  }

  @Test
  void newHighRaisesAnchor_thenConfirms() {
    admit("90", "70");
    List<Candle> candles = history();
    candles.add(candle("97", "100", "96", "97", "50")); // wick 0.75, high 100 >  90
    client.candlesBySymbol.put("PEPEUSDT", candles);

    watcher.run();

    Candidate c = stored();
    assertEquals(CandidateState.CONFIRMED, c.state());
    assertEquals(0, new BigDecimal("100").compareTo(c.anchorHigh()));
  }

  @Test
  void gapWithoutConfirmation_marksMissed() {
    admit("100", "70");
    List<Candle> candles = history();
    candles.add(candle("80", "80", "59", "60", "100")); // close 60 < floor 72
    client.candlesBySymbol.put("PEPEUSDT", candles);

    watcher.run();

    assertEquals(CandidateState.MISSED, stored().state());
  }

  @Test
  void confirmedSameTick_notOverwrittenByGap() {
    admit("100", "70");
    List<Candle> candles = history();

    candles.add(candle("100", "100", "64", "65", "300"));
    client.candlesBySymbol.put("PEPEUSDT", candles);

    watcher.run();

    assertEquals(CandidateState.CONFIRMED, stored().state());
  }

  // --- helpers ---------------------------------------------------------------

  private Candidate stored() {
    return recorder.stored.values().stream().findFirst().orElseThrow();
  }

  private void admit(String anchorHigh, String pumpStart) {
    pool.admit(
        new PumpSignal(
            "PEPEUSDT",
            "PEPE",
            Exchange.BITGET,
            Source.SCAN_4H,
            new Candle(
                T0.minus(Duration.ofMinutes(30)).toEpochMilli(),
                new BigDecimal(pumpStart),
                new BigDecimal(anchorHigh),
                new BigDecimal(pumpStart),
                new BigDecimal(anchorHigh),
                new BigDecimal("100000")),
            T0.minus(Duration.ofMinutes(30))));
  }

  private static List<Candle> history() {
    List<Candle> candles = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
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

  private static final class FakeClient implements MarketDataClient {
    final Map<String, List<Candle>> candlesBySymbol = new java.util.HashMap<>();

    @Override
    public Exchange exchange() {
      return Exchange.BITGET;
    }

    @Override
    public List<Ticker> tickers() {
      return List.of();
    }

    @Override
    public Map<String, SymbolMeta> symbols() {
      return Map.of();
    }

    @Override
    public List<Candle> candles(String symbol, Timeframe timeframe, int limit) {
      return candlesBySymbol.getOrDefault(symbol, List.of());
    }
  }
}
