package com.virginonline.dumpradar.scanner.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.virginonline.dumpradar.config.props.PoolProperties;
import com.virginonline.dumpradar.config.props.PrefilterProperties;
import com.virginonline.dumpradar.config.props.Scanner15mProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.rule.VolumeSurgeRule;
import com.virginonline.dumpradar.scanner.service.CandidatePool;
import com.virginonline.dumpradar.scanner.service.ListingAgeTracker;
import com.virginonline.dumpradar.scanner.service.PriceVelocityTracker;
import com.virginonline.dumpradar.scanner.service.TickerPrefilter;
import com.virginonline.dumpradar.testfix.FakeRecorder;
import com.virginonline.dumpradar.testfix.MutableClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Scanner15mScheduledTest {

  private static final Instant T0 = Instant.parse("2026-09-07T12:00:00Z");

  private final MutableClock clock = new MutableClock(T0);
  private final FakeRecorder recorder = new FakeRecorder();
  private final FakeClient client = new FakeClient();
  private final Scanner15mProperties scannerProps =
      new Scanner15mProperties(new BigDecimal("0.25"), new BigDecimal("0.60"), 8);
  private final CandidatePool pool =
      new CandidatePool(
          recorder,
          clock,
          new PoolProperties(Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(4)),
          (candidate, type, now, entry) -> {});
  private final Scanner15mScheduled scanner =
      new Scanner15mScheduled(
          clock,
          new PrefilterProperties(new BigDecimal("2000000"), new BigDecimal("0.001"), 14, Set.of()),
          new ListingAgeTracker(),
          new TickerPrefilter(),
          List.of(client),
          new PriceVelocityTracker(scannerProps),
          new VolumeSurgeRule(scannerProps),
          pool);

  @Test
  void surgeTickerPlusVolume_admitsScan15mCandidate() {
    scanner.scan();
    clock.advance(Duration.ofMinutes(61));
    client.last = "126";

    scanner.scan();

    Candidate c = recorder.stored.values().stream().findFirst().orElseThrow();
    assertEquals(CandidateState.WATCHING, c.state());
    assertEquals(Source.SCAN_15M, c.source());
    assertEquals("PEPEUSDT", c.symbol());
    assertEquals("PEPE", c.baseAsset());
    assertEquals(0, new BigDecimal("126").compareTo(c.anchorHigh()));
    assertEquals(0, new BigDecimal("100").compareTo(c.pumpStart()));
    assertEquals(T0.plus(Duration.ofMinutes(91)), c.deadline()); // detectedAt + 30m
  }

  private static final class FakeClient implements MarketDataClient {
    String last = "100";

    @Override
    public Exchange exchange() {
      return Exchange.BITGET;
    }

    @Override
    public String chartUrl(String symbol) {
      return "https://www.bitget.com/futures/usdt/" + symbol;
    }

    @Override
    public List<Ticker> tickers() {
      return List.of(
          new Ticker("PEPEUSDT", new BigDecimal(last), 0.2, new BigDecimal("5000000"), 0L));
    }

    @Override
    public Map<String, SymbolMeta> symbols() {
      return Map.of("PEPEUSDT", new SymbolMeta("PEPEUSDT", "PEPE", 0L, "normal"));
    }

    @Override
    public List<Candle> candles(String symbol, Timeframe timeframe, int limit) {
      List<Candle> candles = new ArrayList<>();
      for (int i = 0; i < 60; i++) {
        candles.add(candle("100", "100"));
      }
      for (int i = 0; i < 15; i++) {
        candles.add(candle("120", "800"));
      }
      return candles;
    }

    private static Candle candle(String high, String volume) {
      return new Candle(
          0L,
          new BigDecimal("99"),
          new BigDecimal(high),
          new BigDecimal("98"),
          new BigDecimal(high),
          new BigDecimal(volume));
    }
  }
}
