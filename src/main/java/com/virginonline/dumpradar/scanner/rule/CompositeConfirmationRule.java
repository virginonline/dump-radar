package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.config.props.ConfirmProperties;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Confirmation;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CompositeConfirmationRule implements ConfirmationRule {

  private static final int WINDOW_SIZE = 20;

  private final List<ExhaustionPattern> patterns;
  private final ConfirmProperties props;

  public CompositeConfirmationRule(List<ExhaustionPattern> patterns, ConfirmProperties props) {
    this.patterns = patterns;
    this.props = props;
  }

  @Override
  public Optional<Confirmation> check(Candidate candidate, List<Candle> candles, Instant now) {
    for (int i = WINDOW_SIZE; i < candles.size(); i++) {
      Candle c = candles.get(i);
      if (!isNearAnchor(candidate, c)) continue;
      RuleContext ctx = new RuleContext(candidate, avgVolume(candles, i));
      for (ExhaustionPattern p : patterns) {
        Optional<ConfirmationKind> kind = p.matches(c, ctx);
        if (kind.isPresent()) {
          return Optional.of(new Confirmation(kind.get(), c, now));
        }
      }
    }
    return Optional.empty();
  }

  private boolean isNearAnchor(Candidate candidate, Candle c) {
    return c.high()
            .compareTo(candidate.anchorHigh().multiply(BigDecimal.ONE.subtract(props.nearHigh())))
        >= 0;
  }

  private BigDecimal avgVolume(List<Candle> candles, int i) {
    List<Candle> window = candles.subList(i - WINDOW_SIZE, i);
    BigDecimal sum = BigDecimal.ZERO;
    for (Candle c : window) {
      sum = sum.add(c.volumeQuote());
    }
    return sum.divide(BigDecimal.valueOf(window.size()), 10, RoundingMode.HALF_UP);
  }
}
