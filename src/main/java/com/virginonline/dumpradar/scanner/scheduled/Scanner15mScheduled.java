package com.virginonline.dumpradar.scanner.scheduled;

import com.virginonline.dumpradar.config.props.PrefilterProperties;
import com.virginonline.dumpradar.scanner.exchange.MarketDataClient;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.*;
import com.virginonline.dumpradar.scanner.rule.VolumeSurgeRule;
import com.virginonline.dumpradar.scanner.service.CandidatePool;
import com.virginonline.dumpradar.scanner.service.ListingAgeTracker;
import com.virginonline.dumpradar.scanner.service.PriceVelocityTracker;
import com.virginonline.dumpradar.scanner.service.TickerPrefilter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scanner15mScheduled {

  private static final Logger log = LoggerFactory.getLogger(Scanner15mScheduled.class);

  private final Clock clock;
  private final PrefilterProperties prefilterProps;
  private final ListingAgeTracker ageTracker;
  private final TickerPrefilter prefilter;
  private final List<MarketDataClient> clients;
  private final PriceVelocityTracker velocityTracker;
  private final VolumeSurgeRule volumeSurge;
  private final CandidatePool pool;

  public Scanner15mScheduled(
      Clock clock,
      PrefilterProperties prefilterProperties,
      ListingAgeTracker ageTracker,
      TickerPrefilter filter,
      List<MarketDataClient> clients,
      PriceVelocityTracker velocityTracker,
      VolumeSurgeRule volumeSurge,
      CandidatePool pool) {
    this.clock = clock;
    this.prefilterProps = prefilterProperties;
    this.ageTracker = ageTracker;
    this.prefilter = filter;
    this.clients = clients;
    this.velocityTracker = velocityTracker;
    this.volumeSurge = volumeSurge;
    this.pool = pool;
  }

  @Scheduled(fixedDelay = 10_000, initialDelay = 60_000)
  public void scan() {

    Instant now = clock.instant();
    for (MarketDataClient client : clients) {
      Map<String, SymbolMeta> symbols = ageTracker.enrich(client.symbols(), now);
      List<Ticker> survivors = prefilter.filter(client.tickers(), symbols, prefilterProps, now);

      for (VelocityHit hit : velocityTracker.update(survivors, now)) {
        try {
          List<Candle> candles = client.candles(hit.symbol(), Timeframe.M1, 75);
          if (!volumeSurge.isSurge(candles)) continue;

          BigDecimal anchor =
              candles.stream()
                  .map(Candle::high)
                  .reduce(hit.lastPrice(), BigDecimal::max); // anchor = running max
          Candle synthetic =
              new Candle(
                  now.toEpochMilli(),
                  hit.startPrice(), // open = pump start from snapshot
                  anchor,
                  candles.getLast().low(),
                  candles.getLast().close(),
                  candles.getLast().volumeQuote());
          pool.admit(
              new PumpSignal(
                  hit.symbol(),
                  symbols.get(hit.symbol()).baseCoin(),
                  client.exchange(),
                  Source.SCAN_15M,
                  synthetic,
                  now));
        } catch (Exception e) {
          log.warn("15m scan failed {}: {}", hit.symbol(), e.getMessage());
        }
      }
    }
  }
}
