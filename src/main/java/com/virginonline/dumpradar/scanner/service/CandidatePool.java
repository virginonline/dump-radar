package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.PoolProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.*;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class CandidatePool {
  private static final Logger log = LoggerFactory.getLogger(CandidatePool.class);
  private final Recorder recorder;
  private final Clock clock;
  private final PoolProperties poolProperties;
  private final ObjectMapper mapper;

  public CandidatePool(
      Recorder recorder, Clock clock, PoolProperties poolProperties, ObjectMapper mapper) {
    this.recorder = recorder;
    this.clock = clock;
    this.poolProperties = poolProperties;
    this.mapper = mapper;
  }

  public Decision admit(PumpSignal signal) {
    Instant now = clock.instant();

    Optional<Candidate> active =
        active().stream().filter(c -> c.baseAsset().equals(signal.baseAsset())).findFirst();
    if (active.isPresent()) {
      Candidate current = active.get();
      boolean anchorUpdated = signal.signalCandle().high().compareTo(current.anchorHigh()) > 0;
      BigDecimal newAnchor = anchorUpdated ? signal.signalCandle().high() : current.anchorHigh();

      Set<Exchange> union = new HashSet<>(current.exchanges());
      union.add(signal.exchange());

      Candidate merged =
          new Candidate(
              current.id(),
              current.baseAsset(),
              current.symbol(),
              union,
              current.source(),
              current.state(),
              newAnchor,
              current.pumpStart(),
              current.detectedAt(),
              current.deadline(),
              current.confirmedAt(),
              now);

      recorder.upsertCandidate(merged);
      if (anchorUpdated) {
        recorder.appendEvent(merged.id(), "anchor_update", payloadOf(merged), now);
      }
      return new Decision(merged, false, true, anchorUpdated);
    }
    if (recorder.hasTerminalSince(signal.baseAsset(), now.minus(poolProperties.cooldown()))) {
      return new Decision(null, false, false, false);
    }
    Candidate created =
        new Candidate(
            idOf(signal),
            signal.baseAsset(),
            signal.symbol(),
            Set.of(signal.exchange()),
            signal.source(),
            CandidateState.WATCHING,
            signal.signalCandle().high(),
            signal.signalCandle().open(),
            signal.detectedAt(),
            signal.detectedAt().plus(ttlOf(signal.source())),
            null,
            now);
    recorder.upsertCandidate(created);
    recorder.appendEvent(created.id(), "new", payloadOf(created), now);
    return new Decision(created, true, false, false);
  }

  public void confirm(String candidateId) {
    var candidate = getCandidateById(candidateId);
    Instant now = clock.instant();
    var newCandidate =
        new Candidate(
            candidate.id(),
            candidate.baseAsset(),
            candidate.symbol(),
            candidate.exchanges(),
            candidate.source(),
            CandidateState.CONFIRMED,
            candidate.anchorHigh(),
            candidate.pumpStart(),
            candidate.detectedAt(),
            candidate.deadline(),
            now, // confirmedAt: null ⟺ не CONFIRMED
            now);
    recorder.upsertCandidate(newCandidate);
    recorder.appendEvent(candidate.id(), "confirmed", payloadOf(newCandidate), now);
  }

  public void expireOverdue() {
    Instant now = clock.instant();
    for (Candidate c : active()) {
      if (c.deadline().isAfter(now)) continue;
      Candidate expired =
          new Candidate(
              c.id(),
              c.baseAsset(),
              c.symbol(),
              c.exchanges(),
              c.source(),
              CandidateState.EXPIRED,
              c.anchorHigh(),
              c.pumpStart(),
              c.detectedAt(),
              c.deadline(),
              c.confirmedAt(),
              now);
      recorder.upsertCandidate(expired);
      recorder.appendEvent(c.id(), "expired", payloadOf(expired), now);
    }
  }

  public void markMissed(String candidateId) {
    var candidate = getCandidateById(candidateId);
    Instant now = clock.instant();
    var newCandidate =
        new Candidate(
            candidate.id(),
            candidate.baseAsset(),
            candidate.symbol(),
            candidate.exchanges(),
            candidate.source(),
            CandidateState.MISSED,
            candidate.anchorHigh(),
            candidate.pumpStart(),
            candidate.detectedAt(),
            candidate.deadline(),
            candidate.confirmedAt(),
            now);
    recorder.upsertCandidate(newCandidate);
    recorder.appendEvent(candidate.id(), "missed", payloadOf(newCandidate), now);
  }

  public Candidate getCandidateById(String candidateId) {
    return recorder.loadActive().stream()
        .filter(c -> c.id().equals(candidateId))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("candidate not found"));
  }

  public List<Candidate> active() {
    return recorder.loadActive();
  }

  @EventListener(ApplicationReadyEvent.class)
  public int restore() {
    int active = recorder.loadActive().size();
    log.info("pool restored: {} active candidates survived restart", active);
    return active;
  }

  public Optional<Candidate> raiseAnchor(String candidateId, BigDecimal newHigh) {
    var now = clock.instant();
    var candidate = getCandidateById(candidateId);
    if (newHigh.compareTo(candidate.anchorHigh()) <= 0) return Optional.empty();
    var newCandidate =
        new Candidate(
            candidate.id(),
            candidate.baseAsset(),
            candidate.symbol(),
            candidate.exchanges(),
            candidate.source(),
            candidate.state(),
            newHigh,
            candidate.pumpStart(),
            candidate.detectedAt(),
            candidate.deadline(),
            candidate.confirmedAt(),
            now);
    recorder.upsertCandidate(newCandidate);
    recorder.appendEvent(candidate.id(), "anchor_update", payloadOf(newCandidate), now);
    return Optional.of(newCandidate);
  }

  private String idOf(PumpSignal s) {
    return "%s-%s-%s"
        .formatted(
            s.symbol(),
            s.exchange(),
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(s.detectedAt()));
  }

  private Duration ttlOf(Source source) {
    return source == Source.SCAN_4H ? poolProperties.watching4h() : poolProperties.watching15m();
  }

  private String payloadOf(Candidate c) {
    return mapper.writeValueAsString(
        Map.of(
            "candidateId", c.id(),
            "anchorHigh", c.anchorHigh().toPlainString(),
            "pumpStart", c.pumpStart().toPlainString(),
            "state", c.state().name()));
  }
}
