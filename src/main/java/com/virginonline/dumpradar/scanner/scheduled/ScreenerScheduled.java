package com.virginonline.dumpradar.scanner.scheduled;

import com.virginonline.dumpradar.config.PrefilterProperties;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.PumpSignal;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import com.virginonline.dumpradar.scanner.service.ListingAgeTracker;
import com.virginonline.dumpradar.scanner.service.PumpCandleScreener;
import com.virginonline.dumpradar.scanner.service.TickerPrefilter;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScreenerScheduled {

  private static final Logger log = LoggerFactory.getLogger(ScreenerScheduled.class);
  private final List<MarketDataClient> clients;
  private final TickerPrefilter prefilter;
  private final ListingAgeTracker ageTracker;
  private final PumpCandleScreener screener;
  private final Clock clock;
  private final PrefilterProperties prefilterProperties;
  private final Semaphore semaphore;

  public ScreenerScheduled(
      List<MarketDataClient> clients,
      TickerPrefilter prefilter,
      ListingAgeTracker ageTracker,
      PumpCandleScreener screener,
      Clock clock,
      PrefilterProperties prefilterProperties) {
    this.clients = clients;
    this.prefilter = prefilter;
    this.ageTracker = ageTracker;
    this.screener = screener;
    this.clock = clock;
    this.prefilterProperties = prefilterProperties;
    this.semaphore = new Semaphore(8);
  }

  @Scheduled(cron = "${app.screener.cron}", zone = "UTC")
  public void screener() {
    List<PumpSignal> signals = new ArrayList<>();
    clients.forEach(client -> signals.addAll(scanExchange(client)));
    log.info("scan4h done: exchanges={}, signals={}", clients.size(), signals.size());
  }

  List<PumpSignal> scanExchange(MarketDataClient client) {
    try {
      Instant now = clock.instant();
      Map<String, SymbolMeta> symbols = ageTracker.enrich(client.symbols(), now);
      List<Ticker> survivors =
          prefilter.filter(client.tickers(), symbols, prefilterProperties, now);
      List<PumpSignal> signals = new ArrayList<>();
      AtomicInteger failed = new AtomicInteger();
      try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<Optional<PumpSignal>>> futures =
            survivors.stream()
                .map(t -> pool.submit(() -> screenSymbol(client, t, symbols, now, failed)))
                .toList();
        for (Future<Optional<PumpSignal>> f : futures) {
          try {
            f.get()
                .ifPresent(
                    signal -> {
                      signals.add(signal);
                      sendNotification(signal);
                    });
          } catch (Exception e) {
            log.error(e.getMessage());
          }
        }

        log.info(
            "{} scan4h: scanned={}, signals={}, failed={}",
            client.exchange(),
            survivors.size(),
            signals.size(),
            failed.get());
      }

      return signals;

    } catch (Exception ex) {
      log.error("failed to scan exchange {}", client.exchange().name(), ex);
      return List.of();
    }
  }

  private Optional<PumpSignal> screenSymbol(
      MarketDataClient client,
      Ticker t,
      Map<String, SymbolMeta> symbols,
      Instant now,
      AtomicInteger failed) {
    try {
      semaphore.acquire();
      try {
        return screener
            .screen(client.candles(t.symbol(), Timeframe.H4, 21))
            .map(
                c ->
                    new PumpSignal(
                        t.symbol(),
                        symbols.get(t.symbol()).baseCoin(),
                        client.exchange(),
                        Source.SCAN_4H,
                        c,
                        now));
      } finally {
        semaphore.release();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (Exception e) {
      failed.incrementAndGet();
      log.warn("{}: candles failed: {}", client.exchange(), t.symbol());
      return Optional.empty();
    }
  }

  private void sendNotification(PumpSignal signal) {
    log.info("Sending notification to client: {}", signal);
  }
}
