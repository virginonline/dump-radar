package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.props.Scanner15mProperties;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.model.VelocityHit;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PriceVelocityTracker {

  private final Map<String, Deque<Snapshot>> bySymbol = new HashMap<>();
  private final Map<String, Boolean> surging = new HashMap<>();
  private static final Duration WINDOW_60M = Duration.ofMinutes(60);
  private static final Duration WINDOW_6H = Duration.ofHours(6);
  private final Scanner15mProperties surgeProps;

  public PriceVelocityTracker(Scanner15mProperties surgeProps) {
    this.surgeProps = surgeProps;
  }

  public List<VelocityHit> update(List<Ticker> survivors, Instant now) {
    List<VelocityHit> hits = new ArrayList<>();
    Set<String> alive = new HashSet<>();
    Instant horizon = now.minus(WINDOW_6H);

    survivors.forEach(
        ticker -> {
          alive.add(ticker.symbol());
          Deque<Snapshot> deque =
              bySymbol.computeIfAbsent(ticker.symbol(), k -> new ArrayDeque<>());
          deque.addLast(new Snapshot(now, ticker.last()));

          while (!deque.isEmpty() && deque.peekFirst().at().isBefore(horizon)) {
            deque.removeFirst();
          }
          checkVelocity(ticker, deque, now).ifPresent(hits::add);
        });
    bySymbol.keySet().retainAll(alive);
    surging.keySet().retainAll(alive);

    return hits;
  }

  private Optional<VelocityHit> checkVelocity(Ticker ticker, Deque<Snapshot> deque, Instant now) {
    BigDecimal last = ticker.last();

    BigDecimal start60 = priceAt(deque, now.minus(WINDOW_60M));
    BigDecimal start6h = priceAt(deque, now.minus(WINDOW_6H));

    boolean surge60 =
        start60 != null
            && last.compareTo(start60.multiply(BigDecimal.ONE.add(surgeProps.surge60m()))) >= 0;
    boolean surge6h =
        start6h != null
            && last.compareTo(start6h.multiply(BigDecimal.ONE.add(surgeProps.surge6h()))) >= 0;

    boolean surge = surge60 || surge6h;
    boolean wasSurging = surging.getOrDefault(ticker.symbol(), false);
    surging.put(ticker.symbol(), surge);

    if (!surge || wasSurging) return Optional.empty();

    return Optional.of(new VelocityHit(ticker.symbol(), surge60 ? start60 : start6h, last));
  }

  private BigDecimal priceAt(Deque<Snapshot> deque, Instant boundary) {
    for (Iterator<Snapshot> it = deque.descendingIterator(); it.hasNext(); ) {
      Snapshot s = it.next();
      if (!s.at().isAfter(boundary)) return s.last();
    }
    return null;
  }

  private record Snapshot(Instant at, BigDecimal last) {}
}
