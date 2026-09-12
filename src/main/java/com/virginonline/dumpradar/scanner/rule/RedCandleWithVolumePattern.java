package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RedCandleWithVolumePattern implements ExhaustionPattern {

  @Override
  public Optional<ConfirmationKind> matches(Candle candle, RuleContext ctx) {
    boolean red = candle.close().compareTo(candle.open()) < 0;
    boolean volumeSurge = candle.volumeQuote().compareTo(ctx.avgVolume()) > 0;
    return red && volumeSurge
        ? Optional.of(ConfirmationKind.RED_CANDLE_WITH_VOLUME)
        : Optional.empty();
  }
}
