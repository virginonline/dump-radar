package com.virginonline.dumpradar.testfix;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory Recorder without SQL: upper layers isolated, SQL covered by JdbcRecorderTest. */
public class FakeRecorder implements Recorder {
  public final Map<String, Candidate> stored = new LinkedHashMap<>();
  public final List<String[]> allEvents = new ArrayList<>();
  public final Map<String, List<Candle>> candlesBySymbol = new HashMap<>();

  @Override
  public void upsertCandidate(Candidate candidate) {
    stored.put(candidate.id(), candidate);
  }

  @Override
  public void appendEvent(
      String candidateId, String eventType, String jsonPayload, Instant occurredAt) {
    allEvents.add(new String[] {candidateId, eventType, jsonPayload});
  }

  @Override
  public void appendCandles(String symbol, List<Candle> candles) {
    List<Candle> merged = new ArrayList<>(candlesBySymbol.getOrDefault(symbol, List.of()));
    for (Candle c : candles) {
      if (merged.stream().noneMatch(x -> x.openTime() == c.openTime())) {
        merged.add(c);
      }
    }
    merged.sort(Comparator.comparingLong(Candle::openTime));
    candlesBySymbol.put(symbol, merged);
  }

  @Override
  public List<Candle> candlesOf(String symbol, int limit) {
    List<Candle> all = candlesBySymbol.getOrDefault(symbol, List.of());
    return all.subList(Math.max(0, all.size() - limit), all.size());
  }

  @Override
  public List<Candidate> loadActive() {
    return stored.values().stream().filter(c -> c.state() == CandidateState.WATCHING).toList();
  }

  @Override
  public boolean hasTerminalSince(String baseAsset, Instant since) {
    return stored.values().stream()
        .anyMatch(
            c ->
                c.baseAsset().equals(baseAsset)
                    && c.state() != CandidateState.WATCHING
                    && c.updatedAt().isAfter(since));
  }

  public List<String> eventTypes(String candidateId) {
    return allEvents.stream().filter(e -> e[0].equals(candidateId)).map(e -> e[1]).toList();
  }
}
