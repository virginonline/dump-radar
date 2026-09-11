package com.virginonline.dumpradar.scanner.scheduled;

import com.virginonline.dumpradar.config.ConfirmProperties;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.service.CandidatePool;
import com.virginonline.dumpradar.scanner.service.ConfirmationRule;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WatcherScheduled {
  private static final Logger log = LoggerFactory.getLogger(WatcherScheduled.class);
  private final CandidatePool pool;
  private final List<MarketDataClient> clients;
  private final Clock clock;
  private final ConfirmationRule rule;
  private final ConfirmProperties confirmProps;

  public WatcherScheduled(
      CandidatePool pool,
      List<MarketDataClient> clients,
      Clock clock,
      ConfirmationRule confirmationRule,
      ConfirmProperties confirmProps) {
    this.pool = pool;
    this.clients = clients;
    this.clock = clock;
    this.rule = confirmationRule;
    this.confirmProps = confirmProps;
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
  public void run() {
    var now = clock.instant();
    for (Candidate candidate : pool.active()) {
      try {
        var client = clientFor(candidate);
        List<Candle> candles = client.candles(candidate.symbol(), Timeframe.M1, 120);

        Candidate current =
            candles.stream()
                .map(Candle::high)
                .max(BigDecimal::compareTo)
                .flatMap(high -> pool.raiseAnchor(candidate.id(), high))
                .orElse(candidate);

        boolean confirmed = rule.check(current, candles, now).isPresent();
        if (confirmed) {
          pool.confirm(current.id());
          continue;
        }
        BigDecimal floor =
            current.anchorHigh().multiply(BigDecimal.ONE.subtract(confirmProps.gapDrop()));
        if (candles.getLast().close().compareTo(floor) < 0) {
          pool.markMissed(current.id());
        }
      } catch (Exception e) {
        log.warn("watch failed {}: {}", candidate.id(), e.getMessage());
      }
    }
  }

  private MarketDataClient clientFor(Candidate c) {
    return clients.stream()
        .filter(cl -> c.exchanges().contains(cl.exchange()))
        .findFirst()
        .orElseThrow();
  }
}
