package com.virginonline.dumpradar.scanner.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Source;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sqlite.SQLiteDataSource;

class JdbcRecorderTest {

  private static final Instant T = Instant.parse("2026-09-07T12:00:30Z");

  private JdbcRecorder recorder;
  private NamedParameterJdbcTemplate jdbc;

  @BeforeEach
  void setUp(@TempDir Path tmp) {
    SQLiteDataSource ds = new SQLiteDataSource();
    ds.setUrl("jdbc:sqlite:" + tmp.resolve("test.db"));
    Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
    jdbc = new NamedParameterJdbcTemplate(ds);
    recorder = new JdbcRecorder(jdbc);
  }

  @Test
  void upsert_andLoadActive_roundtrip() {
    Candidate c =
        new Candidate(
            "PEPEUSDT-BITGET-20260907T1200Z",
            "PEPE",
            "PEPEUSDT",
            Set.of(Exchange.BITGET),
            Source.SCAN_4H,
            CandidateState.WATCHING,
            new BigDecimal("0.00001420"),
            new BigDecimal("0.00000995"),
            T,
            T.plusSeconds(86_400),
            null,
            T.plusSeconds(1));

    recorder.upsertCandidate(c);

    List<Candidate> active = recorder.loadActive();
    assertEquals(1, active.size());
    Candidate loaded = active.getFirst();
    assertEquals(c.id(), loaded.id());
    assertEquals(c.baseAsset(), loaded.baseAsset());
    assertEquals(c.exchanges(), loaded.exchanges());
    assertEquals(c.source(), loaded.source());
    assertEquals(c.state(), loaded.state());
    assertEquals(0, c.anchorHigh().compareTo(loaded.anchorHigh()));
    assertEquals(0, c.pumpStart().compareTo(loaded.pumpStart()));
    assertEquals(c.detectedAt(), loaded.detectedAt());
    assertEquals(c.deadline(), loaded.deadline());
    assertNull(loaded.confirmedAt());
    assertEquals(c.updatedAt(), loaded.updatedAt());
  }

  @Test
  void upsertTwice_updatesRowNotDuplicates() {
    recorder.upsertCandidate(watching("0.00001000"));
    recorder.upsertCandidate(watching("0.00002000"));

    List<Candidate> active = recorder.loadActive();
    assertEquals(1, active.size());
    assertEquals(0, new BigDecimal("0.00002000").compareTo(active.getFirst().anchorHigh()));
  }

  @Test
  void expiredCandidate_notInActive() {
    recorder.upsertCandidate(watching("0.00001000"));
    recorder.upsertCandidate(
        new Candidate(
            "PEPEUSDT-BITGET-20260907T1200Z",
            "PEPE",
            "PEPEUSDT",
            Set.of(Exchange.BITGET),
            Source.SCAN_4H,
            CandidateState.EXPIRED,
            new BigDecimal("0.00001000"),
            new BigDecimal("0.00000995"),
            T,
            T.plusSeconds(86_400),
            null,
            T.plusSeconds(60)));

    assertEquals(0, recorder.loadActive().size());
  }

  @Test
  void appendEvent_recordsRowWithTimestamp() {
    recorder.upsertCandidate(watching("0.00001000"));

    recorder.appendEvent(
        "PEPEUSDT-BITGET-20260907T1200Z", "new", "{\"schema\":\"dumpbot.notify/v1\"}", T);

    assertEquals(
        1,
        (int)
            jdbc.getJdbcOperations()
                .queryForObject(
                    "select count(*) from t_events where candidate_id = ?",
                    Integer.class,
                    "PEPEUSDT-BITGET-20260907T1200Z"));
    Long occurredAt =
        jdbc.getJdbcOperations().queryForObject("select occurred_at from t_events", Long.class);
    assertEquals(T.toEpochMilli(), occurredAt);
  }

  @Test
  void appendCandles_idempotent() {
    List<Candle> candles =
        List.of(candle(1_000L, "1.0"), candle(1_060L, "1.1"), candle(1_120L, "0.9"));

    recorder.appendCandles("PEPEUSDT", candles);
    recorder.appendCandles("PEPEUSDT", candles);

    Integer count =
        jdbc.getJdbcOperations()
            .queryForObject(
                "select count(*) from t_candle_1m where symbol = 'PEPEUSDT'", Integer.class);
    assertEquals(3, count);
  }

  @Test
  void terminalSince_cooldownWindow() {
    recorder.upsertCandidate(watching("0.00001000")); // WATCHING — не в счёт
    assertFalse(recorder.hasTerminalSince("PEPE", T));

    recorder.upsertCandidate(terminal(CandidateState.EXPIRED)); // updatedAt = T+60s
    assertTrue(recorder.hasTerminalSince("PEPE", T.plusSeconds(30)));
    assertFalse(recorder.hasTerminalSince("PEPE", T.plusSeconds(120)));
    assertFalse(recorder.hasTerminalSince("BTC", T)); // чужой base_asset
  }

  private static Candidate terminal(CandidateState state) {
    return new Candidate(
        "PEPEUSDT-BITGET-20260907T1200Z",
        "PEPE",
        "PEPEUSDT",
        Set.of(Exchange.BITGET),
        Source.SCAN_4H,
        state,
        new BigDecimal("0.00001000"),
        new BigDecimal("0.00000995"),
        T,
        T.plusSeconds(86_400),
        null,
        T.plusSeconds(60));
  }

  private static Candidate watching(String anchorHigh) {
    return new Candidate(
        "PEPEUSDT-BITGET-20260907T1200Z",
        "PEPE",
        "PEPEUSDT",
        Set.of(Exchange.BITGET),
        Source.SCAN_4H,
        CandidateState.WATCHING,
        new BigDecimal(anchorHigh),
        new BigDecimal("0.00000995"),
        T,
        T.plusSeconds(86_400),
        null,
        T.plusSeconds(1));
  }

  private static Candle candle(long openTime, String close) {
    return new Candle(
        openTime,
        new BigDecimal("1.0"),
        new BigDecimal("1.2"),
        new BigDecimal("0.8"),
        new BigDecimal(close),
        new BigDecimal("500"));
  }
}
