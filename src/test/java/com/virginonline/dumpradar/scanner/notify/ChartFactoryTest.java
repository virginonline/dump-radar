package com.virginonline.dumpradar.scanner.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.testfix.FakeRecorder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ChartFactoryTest {

  private static final Instant T = Instant.parse("2026-09-07T12:00:00Z");

  private final ObjectMapper mapper = new ObjectMapper();
  private final FakeRecorder recorder = new FakeRecorder();

  private final ChartFactory factory =
      new ChartFactory("dump-radar", recorder, mapper, List.of(new StubClient()));

  @BeforeEach
  void seedCandles() {
    recorder.appendCandles(
        "PEPEUSDT", List.of(candle(1_000L, "100"), candle(1_060L, "120"), candle(1_120L, "142")));
  }

  @Test
  void composeNewEvent_fullWireShape() {
    String json = factory.compose(candidate(), EventType.NEW, T, null);

    JsonNode root = mapper.readTree(json);
    assertEquals("dumpbot.notify/v1", root.get("schema").asString());
    assertEquals("dump-radar", root.get("bot").asString());
    assertEquals("candidate.new", root.get("event").asString());
    assertEquals("PEPEUSDT-BITGET-20260907T120000Z", root.get("candidateId").asString());
    assertEquals(T.toString(), root.get("occurredAt").asString());

    JsonNode data = root.get("data");
    assertEquals("4H", data.get("timeframe").asString());
    assertTrue(data.get("title").asString().contains("PEPEUSDT"));
    assertEquals(3, data.get("bars").size());
    assertTrue(data.get("bars").get(0).get(1).isString()); // bar prices are strings
    assertTrue(data.get("stats").get("pumpPct").isNumber());
    assertTrue(data.get("stats").get("rr").isNumber());
    assertEquals(T.plusSeconds(86_400).toString(), data.get("deadline").asString());
    assertEquals(
        "https://www.bitget.com/futures/usdt/PEPEUSDT",
        data.get("urls").get("exchange").asString());

    assertEquals(2, data.get("levels").size());
    assertEquals("anchor", data.get("levels").get(0).get("role").asString());
    assertEquals("stop", data.get("levels").get(1).get("role").asString());
    assertEquals("144.84", data.get("levels").get(1).get("value").asString());
    assertFalse(data.get("levels").toString().contains("entry"));

    JsonNode tps = data.get("takeprofit");
    assertEquals(3, tps.size());
    assertEquals("125.956", tps.get(0).get("value").asString());
    assertEquals("0.40", tps.get(0).get("size").asString());
    assertEquals("stop → break even", tps.get(0).get("note").asString());
    assertTrue(tps.get(2).get("note") == null || tps.get(2).get("note").isNull());
  }

  @Test
  void composeConfirmedWithEntry_entryLevelPresent() {
    String json = factory.compose(candidate(), EventType.CONFIRMED, T, new BigDecimal("138"));

    JsonNode levels = mapper.readTree(json).get("data").get("levels");
    assertEquals(3, levels.size());
    assertEquals("entry", levels.get(1).get("role").asString());
    assertEquals("138", levels.get(1).get("value").asString());
    assertEquals("candidate.confirmed", mapper.readTree(json).get("event").asString());
  }

  private static Candle candle(long openTime, String close) {
    return new Candle(
        openTime,
        new BigDecimal(close),
        new BigDecimal(close),
        new BigDecimal(close),
        new BigDecimal(close),
        new BigDecimal("500"));
  }

  private static Candidate candidate() {
    return new Candidate(
        "PEPEUSDT-BITGET-20260907T120000Z",
        "PEPE",
        "PEPEUSDT",
        Set.of(Exchange.BITGET),
        Source.SCAN_4H,
        CandidateState.WATCHING,
        new BigDecimal("142"),
        new BigDecimal("100"),
        T,
        T.plusSeconds(86_400),
        null,
        T);
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
      return Map.of();
    }
  }
}
