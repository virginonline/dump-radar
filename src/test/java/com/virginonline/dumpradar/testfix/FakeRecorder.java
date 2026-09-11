package com.virginonline.dumpradar.testfix;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FakeRecorder implements Recorder {
  public final Map<String, Candidate> stored = new LinkedHashMap<>();
  public final List<String[]> allEvents = new ArrayList<>();

  @Override
  public void upsertCandidate(Candidate candidate) {
    stored.put(candidate.id(), candidate);
  }

  @Override
  public void appendEvent(
      String candidateId, String eventType, String jsonPayload, Instant occurredAt) {
    allEvents.add(new String[] {candidateId, eventType});
  }

  @Override
  public void appendCandles(String symbol, List<Candle> candles) {}

  @Override
  public List<Candidate> loadActive() {
    return stored.values().stream()
        .filter(c -> c.state() == com.virginonline.dumpradar.scanner.model.CandidateState.WATCHING)
        .toList();
  }

  @Override
  public boolean hasTerminalSince(String baseAsset, Instant since) {
    return stored.values().stream()
        .anyMatch(
            c ->
                c.baseAsset().equals(baseAsset)
                    && c.state() != com.virginonline.dumpradar.scanner.model.CandidateState.WATCHING
                    && c.updatedAt().isAfter(since));
  }

  public List<String> eventTypes(String candidateId) {
    return allEvents.stream().filter(e -> e[0].equals(candidateId)).map(e -> e[1]).toList();
  }
}
