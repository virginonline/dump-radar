package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.config.props.Scanner15mProperties;
import com.virginonline.dumpradar.scanner.model.Candle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VolumeSurgeRule {

  private static final int BLOCK = 15;
  private static final int BLOCKS = 4;

  private final BigDecimal volumeMultiple;

  public VolumeSurgeRule(Scanner15mProperties props) {
    this.volumeMultiple = BigDecimal.valueOf(props.volumeMultiple());
  }

  public boolean isSurge(List<Candle> candles) {
    if (candles.size() < BLOCK * (BLOCKS + 1)) return false;

    BigDecimal recent = blockVolume(candles, candles.size() - BLOCK);

    List<BigDecimal> history = new ArrayList<>();
    for (int b = 0; b < BLOCKS; b++) {
      history.add(blockVolume(candles, b * BLOCK));
    }
    Collections.sort(history);
    BigDecimal median =
        history.get(1).add(history.get(2)).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);

    return recent.compareTo(volumeMultiple.multiply(median)) >= 0;
  }

  private BigDecimal blockVolume(List<Candle> candles, int from) {
    return candles.subList(from, from + BLOCK).stream()
        .map(Candle::volumeQuote)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
