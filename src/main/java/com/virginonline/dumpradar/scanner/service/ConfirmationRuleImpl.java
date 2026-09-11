package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.ConfirmProperties;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Confirmation;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ConfirmationRuleImpl implements ConfirmationRule {
  private final ConfirmProperties confirmProps;

  private final Integer WINDOW_SIZE = 20;
  private final BigDecimal WICK_RATIO = new BigDecimal("0.6");

  public ConfirmationRuleImpl(ConfirmProperties confirmProps) {
    this.confirmProps = confirmProps;
  }

  @Override
  public Optional<Confirmation> check(Candidate candidate, List<Candle> candles, Instant now) {
    for (int i = WINDOW_SIZE; i < candles.size(); i++) {
      Candle c = candles.get(i);
      BigDecimal avgVolume = avgVolume(candles.subList(i - WINDOW_SIZE, i));

      boolean nearAnchor =
          c.high()
                  .compareTo(
                      candidate
                          .anchorHigh()
                          .multiply(BigDecimal.ONE.subtract(confirmProps.nearHigh())))
              >= 0;

      boolean redWithVolume =
          c.close().compareTo(c.open()) < 0 && c.volumeQuote().compareTo(avgVolume) > 0;

      BigDecimal range = c.high().subtract(c.low());
      boolean wickRejection =
          range.signum() > 0 // high == low -> skip
              && c.high()
                      .subtract(c.close().max(c.open()))
                      .divide(range, 10, RoundingMode.HALF_UP)
                      .compareTo(WICK_RATIO)
                  > 0;
      if (nearAnchor && (redWithVolume || wickRejection)) {
        return Optional.of(
            new Confirmation(
                redWithVolume
                    ? ConfirmationKind.RED_CANDLE_WITH_VOLUME
                    : ConfirmationKind.WICK_REJECTION,
                c,
                now));
      }
    }
    return Optional.empty();
  }

  private BigDecimal avgVolume(List<Candle> window) {
    BigDecimal sum = BigDecimal.ZERO;
    for (Candle c : window) {
      sum = sum.add(c.volumeQuote());
    }
    return sum.divide(BigDecimal.valueOf(window.size()), 10, RoundingMode.HALF_UP);
  }
}
