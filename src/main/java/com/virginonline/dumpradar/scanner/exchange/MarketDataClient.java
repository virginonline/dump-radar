package com.virginonline.dumpradar.scanner.exchange;

import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;

import java.util.List;
import java.util.Map;
public interface MarketDataClient {

    List<Ticker> tickers();

    List<Candle> candles(String symbol, Timeframe timeframe, int limit);

    Map<String, SymbolMeta> symbols();

    Exchange exchange();
}
