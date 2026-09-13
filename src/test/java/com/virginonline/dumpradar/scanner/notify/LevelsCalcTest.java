package com.virginonline.dumpradar.scanner.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Source;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LevelsCalcTest {

  private static final Instant T = Instant.parse("2026-09-07T12:00:00Z");

  @Test
  void fibTargets_belowAnchor_nearestFirst() {
    Candidate c = candidate("142", "100");

    LevelsCalc.Levels levels = LevelsCalc.from(c);

    assertEquals(0, new BigDecimal("144.84").compareTo(levels.stop()));
    assertEquals(3, levels.tps().size());
    assertEquals(0, new BigDecimal("125.956").compareTo(levels.tps().get(0).price()));
    assertEquals(0, new BigDecimal("121").compareTo(levels.tps().get(1).price()));
    assertEquals(0, new BigDecimal("116.044").compareTo(levels.tps().get(2).price()));

    assertTrue(levels.tps().get(0).price().compareTo(levels.tps().get(1).price()) > 0);
    assertTrue(levels.tps().get(1).price().compareTo(levels.tps().get(2).price()) > 0);
  }

  @Test
  void sizesAndNotes_carried() {
    LevelsCalc.Levels levels = LevelsCalc.from(candidate("142", "100"));

    assertEquals(0, new BigDecimal("0.40").compareTo(levels.tps().get(0).size()));
    assertEquals("stop → break even", levels.tps().get(0).note());
    assertEquals("stop → TP1", levels.tps().get(1).note());
    assertEquals(null, levels.tps().get(2).note());
  }

  @Test
  void zeroStretch_doesNotCrash() {
    LevelsCalc.Levels levels = LevelsCalc.from(candidate("100", "100"));

    assertEquals(0, new BigDecimal("102").compareTo(levels.stop()));
    levels.tps().forEach(tp -> assertEquals(0, new BigDecimal("100").compareTo(tp.price())));
  }

  private static Candidate candidate(String anchor, String pumpStart) {
    return new Candidate(
        "PEPEUSDT-BITGET-20260907T120000Z",
        "PEPE",
        "PEPEUSDT",
        Set.of(Exchange.BITGET),
        Source.SCAN_4H,
        CandidateState.WATCHING,
        new BigDecimal(anchor),
        new BigDecimal(pumpStart),
        T,
        T.plusSeconds(86_400),
        null,
        T);
  }
}
