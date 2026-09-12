package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Confirmation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConfirmationRule {
  Optional<Confirmation> check(Candidate candidate, List<Candle> candles, Instant now);
}
