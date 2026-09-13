package com.virginonline.dumpradar.scanner.notify;

import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ChartFactory {

  private static final String SCHEMA = "dumpbot.notify/v1";
  private static final int BARS = 120;
  private final String bot;
  private final Recorder recorder;
  private final ObjectMapper mapper;
  private final List<MarketDataClient> clients;

  public ChartFactory(
      @Value("${spring.application.name}") String bot,
      Recorder recorder,
      ObjectMapper mapper,
      List<MarketDataClient> clients) {
    this.bot = bot;
    this.recorder = recorder;
    this.mapper = mapper;
    this.clients = clients;
  }

  private static String timeframeOf(Source source) {
    return source == Source.SCAN_4H ? "4H" : "15m";
  }

  private static Map<String, Object> statsOf(Candidate c) {
    BigDecimal stretch = c.anchorHigh().subtract(c.pumpStart());
    double pumpPct = stretch.doubleValue() / c.pumpStart().doubleValue();
    double rr =
        stretch
            .multiply(new BigDecimal("0.48"))
            .divide(c.anchorHigh().multiply(new BigDecimal("0.02")), 6, RoundingMode.HALF_UP)
            .doubleValue();

    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("pumpPct", pumpPct);
    stats.put("rr", rr);
    return stats;
  }

  private static List<Object> barToArray(Candle c) {
    return List.of(
        c.openTime(),
        c.open().toPlainString(),
        c.high().toPlainString(),
        c.low().toPlainString(),
        c.close().toPlainString(),
        c.volumeQuote().toPlainString());
  }

  private static List<Map<String, Object>> levelsOf(
      Candidate c, LevelsCalc.Levels levels, BigDecimal entry) {
    List<Map<String, Object>> out = new ArrayList<>();
    out.add(level("anchor", c.anchorHigh(), null));
    if (entry != null) {
      out.add(level("entry", entry, "market if accepted"));
    }
    out.add(level("stop", levels.stop(), "high +2%"));
    return out;
  }

  private static Map<String, Object> level(String role, BigDecimal value, String label) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("role", role);
    m.put("value", value.toPlainString());
    if (label != null) {
      m.put("label", label);
    }
    return m;
  }

  private static List<Map<String, Object>> takeProfitArray(LevelsCalc.Levels levels) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (LevelsCalc.TakeProfit tp : levels.tps()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("value", tp.price().toPlainString());
      m.put("pct", tp.pct().toPlainString());
      m.put("size", tp.size().toPlainString());
      if (tp.note() != null) {
        m.put("note", tp.note());
      }
      out.add(m);
    }
    return out;
  }

  public String compose(Candidate c, EventType type, Instant now, BigDecimal entry) {
    LevelsCalc.Levels levels = LevelsCalc.from(c);
    List<Candle> bars = recorder.candlesOf(c.symbol(), BARS);

    Map<String, Object> event = new LinkedHashMap<>();
    event.put("schema", SCHEMA);
    event.put("bot", bot);
    event.put("event", type.wireName());
    event.put("candidateId", c.id());
    event.put("occurredAt", now.toString());

    Map<String, Object> data = new LinkedHashMap<>();
    data.put(
        "title",
        "%s . %s . %s"
            .formatted(
                c.symbol(),
                timeframeOf(c.source()),
                String.join(",", c.exchanges().stream().map(Enum::name).toList())));
    data.put("timeframe", timeframeOf(c.source()));
    data.put("stats", statsOf(c));
    data.put("bars", bars.stream().map(ChartFactory::barToArray).toList());
    data.put("levels", levelsOf(c, levels, entry));
    data.put("takeprofit", takeProfitArray(levels));
    data.put("deadline", c.deadline().toString());
    data.put("urls", Map.of("exchange", chartUrlOf(c)));
    event.put("data", data);

    return mapper.writeValueAsString(event);
  }

  private String chartUrlOf(Candidate c) {
    return clients.stream()
        .filter(cl -> c.exchanges().contains(cl.exchange()))
        .findFirst()
        .map(cl -> cl.chartUrl(c.symbol()))
        .orElse("");
  }
}
