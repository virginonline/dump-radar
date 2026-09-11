package com.virginonline.dumpradar.scanner.repository;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Source;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRecorder implements Recorder {
  private static final String UPSERT_CANDIDATE =
      """
              insert into t_candidates (id, base_asset, symbol, exchanges, source, state,
                                        anchor_high, pump_start, detected_at, deadline, confirmed_at, updated_at)
              values (:id, :baseAsset, :symbol, :exchanges, :source, :state,
                      :anchorHigh, :pumpStart, :detectedAt, :deadline, :confirmedAt, :updatedAt)
              on conflict(id) do update set
                  exchanges    = excluded.exchanges,
                  state        = excluded.state,
                  anchor_high  = excluded.anchor_high,
                  pump_start   = excluded.pump_start,
                  deadline     = excluded.deadline,
                  confirmed_at = excluded.confirmed_at,
                  updated_at   = excluded.updated_at
              """;
  private final NamedParameterJdbcTemplate jdbc;

  public JdbcRecorder(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static Instant instant(ResultSet rs, String col) throws SQLException {
    long v = rs.getLong(col);
    return rs.wasNull() ? null : Instant.ofEpochMilli(v);
  }

  private static Set<Exchange> parseExchanges(String csv) {
    return Arrays.stream(csv.split(",")).map(Exchange::valueOf).collect(Collectors.toSet());
  }

  @Override
  public void upsertCandidate(Candidate c) {
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", c.id())
            .addValue("baseAsset", c.baseAsset())
            .addValue("symbol", c.symbol())
            .addValue(
                "exchanges",
                c.exchanges().stream().map(Enum::name).collect(Collectors.joining(",")))
            .addValue("source", c.source().name())
            .addValue("state", c.state().name())
            .addValue("anchorHigh", c.anchorHigh().toPlainString())
            .addValue("pumpStart", c.pumpStart().toPlainString())
            .addValue("detectedAt", c.detectedAt().toEpochMilli())
            .addValue("deadline", c.deadline().toEpochMilli())
            .addValue(
                "confirmedAt", c.confirmedAt() == null ? null : c.confirmedAt().toEpochMilli())
            .addValue("updatedAt", c.updatedAt().toEpochMilli());
    jdbc.update(UPSERT_CANDIDATE, params);
  }

  @Override
  public void appendEvent(
      String candidateId, String eventType, String jsonPayload, Instant occurredAt) {
    jdbc.update(
        "insert into t_events (candidate_id, event_type, payload, occurred_at) "
            + "values (:candidateId, :eventType, :payload, :occurredAt)",
        new MapSqlParameterSource()
            .addValue("candidateId", candidateId)
            .addValue("eventType", eventType)
            .addValue("payload", jsonPayload)
            .addValue("occurredAt", occurredAt.toEpochMilli()));
  }

  @Override
  public void appendCandles(String symbol, List<Candle> candles) {
    jdbc.getJdbcOperations()
        .batchUpdate(
            "insert or ignore into t_candle_1m (symbol, open_time, open, high, low, close, volume) values (?,?,?,?,?,?,?)",
            candles,
            candles.size(),
            (ps, c) -> {
              ps.setString(1, symbol);
              ps.setLong(2, c.openTime());
              ps.setString(3, c.open().toPlainString());
              ps.setString(4, c.high().toPlainString());
              ps.setString(5, c.low().toPlainString());
              ps.setString(6, c.close().toPlainString());
              ps.setString(7, c.volumeQuote().toPlainString());
            });
  }

  @Override
  public List<Candidate> loadActive() {
    return jdbc.query(
        "select * from t_candidates where state = 'WATCHING'",
        (rs, n) ->
            new Candidate(
                rs.getString("id"),
                rs.getString("base_asset"),
                rs.getString("symbol"),
                parseExchanges(rs.getString("exchanges")),
                Source.valueOf(rs.getString("source")),
                CandidateState.valueOf(rs.getString("state")),
                new BigDecimal(rs.getString("anchor_high")),
                new BigDecimal(rs.getString("pump_start")),
                instant(rs, "detected_at"),
                instant(rs, "deadline"),
                instant(rs, "confirmed_at"),
                instant(rs, "updated_at")));
  }

  @Override
  public boolean hasTerminalSince(String baseAsset, Instant since) {
    Integer count =
        jdbc.getJdbcOperations()
            .queryForObject(
                """
                            select count(*) from t_candidates
                            where base_asset = ?
                            and state != 'WATCHING'
                            and updated_at > ?
                            """,
                Integer.class,
                baseAsset,
                since.toEpochMilli());
    return count != null && count > 0;
  }
}
