package com.virginonline.dumpradar.scanner.notify;

import com.virginonline.dumpradar.scanner.model.Candidate;
import java.math.BigDecimal;
import java.time.Instant;

public interface EventPublisher {
  void publish(Candidate candidate, EventType type, Instant now, BigDecimal entry);
}
