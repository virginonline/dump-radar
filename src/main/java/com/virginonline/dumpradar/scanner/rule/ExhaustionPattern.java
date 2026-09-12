package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.ConfirmationKind;
import java.util.Optional;

public interface ExhaustionPattern {
  Optional<ConfirmationKind> matches(Candle candle, RuleContext ctx);
}
