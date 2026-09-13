package com.virginonline.dumpradar.scanner.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.virginonline.dumpradar.config.props.PoolProperties;
import com.virginonline.dumpradar.config.props.PrefilterProperties;
import com.virginonline.dumpradar.config.props.ScreenProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.PumpSignal;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import com.virginonline.dumpradar.scanner.service.CandidatePool;
import com.virginonline.dumpradar.scanner.service.ListingAgeTracker;
import com.virginonline.dumpradar.scanner.service.PumpCandleScreener;
import com.virginonline.dumpradar.scanner.service.TickerPrefilter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;

class ScreenerScheduledTest {

  private static final Instant NOW = Instant.parse("2026-09-07T12:00:30Z");

  private final ScreenerScheduled job =
      new ScreenerScheduled(
          List.of(),
          new TickerPrefilter(),
          new ListingAgeTracker(),
          new PumpCandleScreener(screenProps()),
          Clock.fixed(NOW, ZoneOffset.UTC),
          prefilterProps(),
          new CandidatePool(
              new NoopRecorder(),
              Clock.fixed(NOW, ZoneOffset.UTC),
              new PoolProperties(Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(4)),
              (candidate, type, now, entry) -> {}));

  @Test
  void cascade_prefiltersThenSignals() {
    FakeClient client =
        new FakeClient(
            List.of(ticker("PUMPUSDT", "10", "5000000"), ticker("DUSTUSDT", "10", "100")),
            Map.of(
                "PUMPUSDT", meta("PUMPUSDT", "PUMP"),
                "DUSTUSDT", meta("DUSTUSDT", "DUST")),
            Map.of("PUMPUSDT", pumpCandles()),
            null);

    List<PumpSignal> signals = job.scanExchange(client);

    assertEquals(1, signals.size());
    assertEquals("PUMPUSDT", signals.getFirst().symbol());
    assertEquals("PUMP", signals.getFirst().baseAsset());
    assertEquals(Exchange.BITGET, signals.getFirst().exchange());
    assertEquals(Source.SCAN_4H, signals.getFirst().source());
    assertEquals(NOW, signals.getFirst().detectedAt());
    assertEquals(List.of("PUMPUSDT"), List.copyOf(client.candleRequests));
  }

  @Test
  void failedSymbol_doesNotKillScan() {
    FakeClient client =
        new FakeClient(
            List.of(ticker("BADUSDT", "10", "5000000"), ticker("OKUSDT", "10", "5000000")),
            Map.of(
                "BADUSDT", meta("BADUSDT", "BAD"),
                "OKUSDT", meta("OKUSDT", "OK")),
            Map.of("OKUSDT", pumpCandles()),
            "BADUSDT");

    List<PumpSignal> signals = job.scanExchange(client);

    assertEquals(1, signals.size());
    assertEquals("OKUSDT", signals.getFirst().symbol());
  }

  private static ScreenProperties screenProps() {
    return new ScreenProperties(
        new BigDecimal("0.30"), new BigDecimal("5"), new BigDecimal("0.25"), 20);
  }

  private static PrefilterProperties prefilterProps() {
    return new PrefilterProperties(
        new BigDecimal("2000000"), new BigDecimal("0.001"), 14, Set.of());
  }

  private static Ticker ticker(String symbol, String last, String volume) {
    return new Ticker(symbol, new BigDecimal(last), 0.1, new BigDecimal(volume), 0L);
  }

  private static SymbolMeta meta(String symbol, String baseCoin) {
    return new SymbolMeta(symbol, baseCoin, 0L, "normal");
  }

  private static List<Candle> pumpCandles() {
    List<Candle> candles = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      candles.add(new Candle(0L, bd("100"), bd("101"), bd("99"), bd("100"), bd("100")));
    }
    candles.add(new Candle(0L, bd("100"), bd("140"), bd("99"), bd("134.9"), bd("600")));
    return candles;
  }

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  private static final class FakeClient implements MarketDataClient {
    final Queue<String> candleRequests = new ConcurrentLinkedQueue<>();
    private final List<Ticker> tickers;
    private final Map<String, SymbolMeta> symbols;
    private final Map<String, List<Candle>> candlesBySymbol;
    private final String failingSymbol;

    FakeClient(
        List<Ticker> tickers,
        Map<String, SymbolMeta> symbols,
        Map<String, List<Candle>> candlesBySymbol,
        String failingSymbol) {
      this.tickers = tickers;
      this.symbols = symbols;
      this.candlesBySymbol = candlesBySymbol;
      this.failingSymbol = failingSymbol;
    }

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
      return tickers;
    }

    @Override
    public Map<String, SymbolMeta> symbols() {
      return symbols;
    }

    @Override
    public List<Candle> candles(String symbol, Timeframe timeframe, int limit) {
      candleRequests.add(symbol);
      if (symbol.equals(failingSymbol)) {
        throw new RuntimeException("boobs");
      }
      return candlesBySymbol.getOrDefault(symbol, List.of());
    }
  }

  private static final class NoopRecorder implements Recorder {
    @Override
    public void upsertCandidate(Candidate candidate) {}

    @Override
    public void appendEvent(
        String candidateId, String eventType, String jsonPayload, Instant occurredAt) {}

    @Override
    public void appendCandles(String symbol, List<Candle> candles) {}

    @Override
    public List<Candle> candlesOf(String symbol, int limit) {
      return List.of();
    }

    @Override
    public List<Candidate> loadActive() {
      return List.of();
    }

    @Override
    public boolean hasTerminalSince(String baseAsset, Instant since) {
      return false;
    }
  }
}
