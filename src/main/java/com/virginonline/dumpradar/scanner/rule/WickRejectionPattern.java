package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class WickRejectionPattern implements ExhaustionPattern {

  private static final BigDecimal WICK_RATIO = new BigDecimal("0.6");

  @Override
  public Optional<ConfirmationKind> matches(Candle candle, RuleContext ctx) {
    BigDecimal range = candle.high().subtract(candle.low());
    if (range.signum() <= 0) return Optional.empty();

    BigDecimal upperWick = candle.high().subtract(candle.close().max(candle.open()));
    boolean rejected = upperWick.divide(range, 10, RoundingMode.HALF_UP).compareTo(WICK_RATIO) > 0;
    return rejected ? Optional.of(ConfirmationKind.WICK_REJECTION) : Optional.empty();
  }
}
