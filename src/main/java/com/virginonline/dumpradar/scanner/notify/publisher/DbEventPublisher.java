package com.virginonline.dumpradar.scanner.notify.publisher;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.notify.ChartFactory;
import com.virginonline.dumpradar.scanner.notify.EventPublisher;
import com.virginonline.dumpradar.scanner.notify.EventType;
import com.virginonline.dumpradar.scanner.repository.Recorder;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DbEventPublisher implements EventPublisher {
  private final ChartFactory factory;
  private final Recorder recorder;

  public DbEventPublisher(ChartFactory factory, Recorder recorder) {
    this.factory = factory;
    this.recorder = recorder;
  }

  @Override
  public void publish(Candidate candidate, EventType type, Instant now, BigDecimal entry) {
    recorder.appendEvent(
        candidate.id(), type.wireName(), factory.compose(candidate, type, now, entry), now);
  }
}
