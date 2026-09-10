package com.virginonline.dumpradar.scanner.repository;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import java.time.Instant;
import java.util.List;

public interface Recorder {

  void upsertCandidate(Candidate candidate);

  void appendEvent(String candidateId, String eventType, String jsonPayload, Instant occurredAt);

  void appendCandles(String symbol, List<Candle> candles);

  List<Candidate> loadActive();

  boolean hasTerminalSince(String baseAsset, Instant since);
}
