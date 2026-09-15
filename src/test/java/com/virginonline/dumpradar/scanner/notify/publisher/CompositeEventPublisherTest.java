package com.virginonline.dumpradar.scanner.notify.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.notify.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompositeEventPublisherTest {

  private static final Instant T = Instant.parse("2026-09-14T12:00:00Z");

  @Test
  void publishesToAllDelegates() {
    RecordingPublisher first = new RecordingPublisher();
    RecordingPublisher second = new RecordingPublisher();
    CompositeEventPublisher composite = new CompositeEventPublisher(List.of(first, second));

    composite.publish(candidate(), EventType.NEW, T, null);

    assertEquals(1, first.calls);
    assertEquals(1, second.calls);
  }

  @Test
  void failingDelegate_doesNotStopOthers() {
    RecordingPublisher survivor = new RecordingPublisher();
    CompositeEventPublisher composite =
        new CompositeEventPublisher(List.of(new ThrowingPublisher(), survivor));

    composite.publish(candidate(), EventType.MISSED, T, null);

    assertEquals(1, survivor.calls);
  }

  private static final class RecordingPublisher
      implements com.virginonline.dumpradar.scanner.notify.EventPublisher {
    int calls;

    @Override
    public void publish(Candidate candidate, EventType type, Instant now, BigDecimal entry) {
      calls++;
    }
  }

  private static final class ThrowingPublisher
      implements com.virginonline.dumpradar.scanner.notify.EventPublisher {
    @Override
    public void publish(Candidate candidate, EventType type, Instant now, BigDecimal entry) {
      throw new IllegalStateException("boom");
    }
  }

  private Candidate candidate() {
    return new Candidate(
        "PEPEUSDT-BITGET-20260914T120000Z",
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
}
