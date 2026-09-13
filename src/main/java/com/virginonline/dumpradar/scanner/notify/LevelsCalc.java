package com.virginonline.dumpradar.scanner.notify;

import com.virginonline.dumpradar.scanner.model.Candidate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class LevelsCalc {

  public static Levels from(Candidate c) {
    BigDecimal stretch = c.anchorHigh().subtract(c.pumpStart());
    BigDecimal stop = c.anchorHigh().multiply(new BigDecimal("1.02"));

    List<TakeProfit> tps = new ArrayList<>();
    tps.add(tp(c, stretch, "0.382", "0.40", "stop → break even"));
    tps.add(tp(c, stretch, "0.500", "0.30", "stop → TP1"));
    tps.add(tp(c, stretch, "0.618", "0.30", null));
    return new Levels(stop, tps);
  }

  private static TakeProfit tp(
      Candidate c, BigDecimal stretch, String pct, String size, String note) {
    BigDecimal retracement = new BigDecimal(pct).multiply(stretch);
    return new TakeProfit(
        c.anchorHigh().subtract(retracement), new BigDecimal(pct), new BigDecimal(size), note);
  }

  public record TakeProfit(BigDecimal price, BigDecimal pct, BigDecimal size, String note) {}

  public record Levels(BigDecimal stop, List<TakeProfit> tps) {}
}
