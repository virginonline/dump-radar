package com.virginonline.dumpradar.scanner.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.virginonline.dumpradar.config.props.PoolProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Confirmation;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import com.virginonline.dumpradar.scanner.model.PumpSignal;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.notify.publisher.DbEventPublisher;
import com.virginonline.dumpradar.scanner.service.CandidatePool;
import com.virginonline.dumpradar.testfix.FakeRecorder;
import com.virginonline.dumpradar.testfix.MutableClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** End-to-end: pool transition -> DbEventPublisher -> full notify/v1 JSON in sink. */
class PublishFlowTest {

  private static final Instant T0 = Instant.parse("2026-09-07T12:00:00Z");

  private final ObjectMapper mapper = new ObjectMapper();
  private final FakeRecorder recorder = new FakeRecorder();
  private final MutableClock clock = new MutableClock(T0);
  private final CandidatePool pool =
      new CandidatePool(
          recorder,
          clock,
          new PoolProperties(Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(4)),
          new DbEventPublisher(
              new ChartFactory("dump-radar", recorder, mapper, List.of(new StubClient())),
              recorder));

  @BeforeEach
  void seedBars() {
    recorder.appendCandles(
        "PEPEUSDT", List.of(candle(1_000L, "100"), candle(1_060L, "120"), candle(1_120L, "142")));
  }

  @Test
  void admitThenConfirm_fullNotifyEventsInSink() {
    var decision = pool.admit(signal());
    clock.advance(Duration.ofMinutes(90));
    pool.confirm(
        decision.candidate().id(),
        new Confirmation(ConfirmationKind.WICK_REJECTION, candle(1_180L, "138.4"), T0));

    assertEquals(2, recorder.allEvents.size());
    JsonNode first = mapper.readTree(recorder.allEvents.get(0)[2]);
    JsonNode second = mapper.readTree(recorder.allEvents.get(1)[2]);

    assertEquals("dumpbot.notify/v1", first.get("schema").asString());
    assertEquals("candidate.new", first.get("event").asString());
    assertEquals("dump-radar", first.get("bot").asString());
    assertEquals(3, first.get("data").get("bars").size());
    assertFalse(first.get("data").get("levels").toString().contains("entry"));

    assertEquals("candidate.confirmed", second.get("event").asString());
    JsonNode entryLevel = null;
    for (JsonNode l : second.get("data").get("levels")) {
      if ("entry".equals(l.get("role").asString())) {
        entryLevel = l;
      }
    }
    assertEquals("138.4", entryLevel.get("value").asString());
  }

  private static PumpSignal signal() {
    return new PumpSignal(
        "PEPEUSDT",
        "PEPE",
        Exchange.BITGET,
        Source.SCAN_4H,
        new Candle(
            T0.minus(Duration.ofMinutes(30)).toEpochMilli(),
            new BigDecimal("100"),
            new BigDecimal("142"),
            new BigDecimal("100"),
            new BigDecimal("142"),
            new BigDecimal("100000")),
        T0.minus(Duration.ofMinutes(30)));
  }

  private static Candle candle(long openTime, String price) {
    return new Candle(
        openTime,
        new BigDecimal(price),
        new BigDecimal(price),
        new BigDecimal(price),
        new BigDecimal(price),
        new BigDecimal("500"));
  }

  private static final class StubClient implements MarketDataClient {
    @Override
    public Exchange exchange() {
      return Exchange.BITGET;
    }

    @Override
    public List<Ticker> tickers() {
      return List.of();
    }

    @Override
    public List<Candle> candles(String symbol, Timeframe timeframe, int limit) {
      return List.of();
    }

    @Override
    public String chartUrl(String symbol) {
      return "https://www.bitget.com/futures/usdt/" + symbol;
    }

    @Override
    public Map<String, SymbolMeta> symbols() {
      return java.util.Map.of();
    }
  }
}
