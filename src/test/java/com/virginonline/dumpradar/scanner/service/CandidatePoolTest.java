package com.virginonline.dumpradar.scanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.config.PoolProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Decision;
import com.virginonline.dumpradar.scanner.model.PumpSignal;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CandidatePoolTest {

  private static final Instant T0 = Instant.parse("2026-09-07T12:00:30Z");

  private final MutableClock clock = new MutableClock(T0);
  private final FakeRecorder recorder = new FakeRecorder();
  private final CandidatePool pool =
      new CandidatePool(
          recorder,
          clock,
          new PoolProperties(Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(4)),
          new ObjectMapper());

  @Test
  void admit_newCandidate_createdWatching() {
    Decision d = pool.admit(signal("PEPE", "0.0000142", "0.00000995"));

    assertTrue(d.created());
    Candidate c = d.candidate();
    assertEquals("PEPEUSDT-BITGET-20260907T120030Z", c.id());
    assertEquals(CandidateState.WATCHING, c.state());
    assertEquals(0, new BigDecimal("0.0000142").compareTo(c.anchorHigh()));
    assertEquals(0, new BigDecimal("0.00000995").compareTo(c.pumpStart()));
    assertEquals(T0.plus(Duration.ofHours(24)), c.deadline()); // ttl(SCAN_4H)
    assertEquals(1, pool.active().size());
    assertEquals(List.of("new"), recorder.eventTypes("PEPEUSDT-BITGET-20260907T120030Z"));
  }

  @Test
  void admit_sameSignalAgain_silentMerge() {
    Decision first = pool.admit(signal("PEPE", "0.0000142", "0.00000995"));

    Decision d = pool.admit(signal("PEPE", "0.0000142", "0.00000995"));

    assertFalse(d.created());
    assertTrue(d.merged());
    assertFalse(d.anchorUpdated());
    assertEquals(1, pool.active().size());
    assertEquals(List.of("new"), recorder.eventTypes(first.candidate().id()));
  }

  @Test
  void admit_higherHigh_updatesAnchor() {
    Decision first = pool.admit(signal("PEPE", "0.0000142", "0.00000995"));

    Decision d = pool.admit(signal("PEPE", "0.0000188", "0.00000995"));

    assertTrue(d.anchorUpdated());
    assertEquals(0, new BigDecimal("0.0000188").compareTo(d.candidate().anchorHigh()));
    assertEquals(List.of("new", "anchor_update"), recorder.eventTypes(first.candidate().id()));
  }

  @Test
  void admit_afterExpiryWithinCooldown_rejected() {
    pool.admit(signal("PEPE", "0.0000142", "0.00000995"));
    clock.advance(Duration.ofHours(25));
    pool.expireOverdue();

    Decision d = pool.admit(signal("PEPE", "0.0000142", "0.00000995"));

    assertNull(d.candidate());
    assertFalse(d.created());
    assertEquals(0, pool.active().size());
    assertEquals(2, recorder.allEvents.size());
  }

  @Test
  void admit_afterCooldown_newCycle() {
    pool.admit(signal("PEPE", "0.0000142", "0.00000995"));
    clock.advance(Duration.ofHours(25));
    pool.expireOverdue();
    clock.advance(Duration.ofHours(4).plusSeconds(1));

    Decision d = pool.admit(signal("PEPE", "0.0000200", "0.0000150"));

    assertTrue(d.created());
    assertEquals(1, pool.active().size());
  }

  @Test
  void confirm_setsConfirmedAtAndState() {
    Decision d = pool.admit(signal("PEPE", "0.0000142", "0.00000995"));
    clock.advance(Duration.ofMinutes(90));

    pool.confirm(d.candidate().id());

    Candidate confirmed = recorder.stored.get(d.candidate().id());
    assertEquals(CandidateState.CONFIRMED, confirmed.state());
    assertEquals(T0.plus(Duration.ofMinutes(90)), confirmed.confirmedAt());
    assertEquals(0, pool.active().size());
  }

  @Test
  void restore_countsActive() {
    pool.admit(signal("PEPE", "0.0000142", "0.00000995"));
    assertEquals(1, pool.restore());
  }

  private static PumpSignal signal(String baseAsset, String high, String open) {
    return new PumpSignal(
        baseAsset + "USDT",
        baseAsset,
        Exchange.BITGET,
        Source.SCAN_4H,
        new Candle(
            T0.toEpochMilli(),
            new BigDecimal(open),
            new BigDecimal(high),
            new BigDecimal(open),
            new BigDecimal(high),
            new BigDecimal("100000")),
        T0);
  }

  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  static final class FakeRecorder implements Recorder {
    final Map<String, Candidate> stored = new LinkedHashMap<>();
    final List<String[]> allEvents = new ArrayList<>();

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

    List<String> eventTypes(String candidateId) {
      return allEvents.stream().filter(e -> e[0].equals(candidateId)).map(e -> e[1]).toList();
    }
  }
}
