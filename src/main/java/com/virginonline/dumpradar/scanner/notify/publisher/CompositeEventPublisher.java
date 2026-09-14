package com.virginonline.dumpradar.scanner.notify.publisher;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.notify.EventPublisher;
import com.virginonline.dumpradar.scanner.notify.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CompositeEventPublisher implements EventPublisher {
  private final List<EventPublisher> delegates;
  private static final Logger log = LoggerFactory.getLogger(CompositeEventPublisher.class);

  public CompositeEventPublisher(List<EventPublisher> delegates) {
    this.delegates = delegates;
  }

  @Override
  public void publish(Candidate candidate, EventType type, Instant now, BigDecimal entry) {
    delegates.forEach(
        eventPublisher -> {
          try {
            eventPublisher.publish(candidate, type, now, entry);
          } catch (Exception e) {
            log.warn(
                "publisher {} failed for candidate {} ({})",
                eventPublisher.getClass().getSimpleName(),
                candidate.id(),
                type,
                e);
          }
        });
  }
}
