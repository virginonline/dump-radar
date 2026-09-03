package com.virginonline.dumpradar.scanner.exchange;

import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Ticker;

import java.util.List;

public interface MarketDataClient {

    List<Ticker> tickers();

    List<Candle> candles(String symbol, Timeframe timeframe, int limit);

    Exchange exchange();
}
