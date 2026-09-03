package com.virginonline.dumpradar.scanner.exchange.impl;

import com.virginonline.dumpradar.config.ExchangeProperties;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Ticker;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class BitgetClientCandlesTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock = Clock.systemUTC();
    private final BitgetClient client = new BitgetClient(
            new ExchangeProperties(new ExchangeProperties.Cache(Duration.ofHours(1)),
                    Map.of(Exchange.BITGET, new ExchangeProperties.Endpoint("https://api.bitget.com"))),
            new OkHttpClient(),
            mapper, clock);

    @Test
    void parseCandles_mapsRowsInChronologicalOrder() {
        JsonNode data = fixture("/fixtures/bitget-candles.json").get("data");

        List<Candle> candles = BitgetClient.Parser.parseCandles(data);

        assertEquals(3, candles.size());
        assertEquals(1788048000000L, candles.get(0).openTime());
        assertEquals(1788076800000L, candles.get(2).openTime());
        assertEquals(new BigDecimal("78197.9"), candles.get(0).open());
        assertEquals(new BigDecimal("78101.3"), candles.get(0).close());
        assertEquals(new BigDecimal("110169827.60529"), candles.get(0).volumeQuote());
    }

    @Test
    void parseTickers_mapsTickerArray() {
        JsonNode data = fixture("/fixtures/bitget-tickers.json").get("data");

        List<Ticker> tickers = BitgetClient.Parser.parseTickers(data);

        assertEquals(2, tickers.size());
        Ticker btc = tickers.get(0);
        assertEquals("BTCUSDT", btc.symbol());
        assertEquals(new BigDecimal("78049.9"), btc.last());
        assertEquals(0.00599, btc.change24h());
        assertEquals(new BigDecimal("793127812.26764"), btc.volumeQuote());
        assertEquals(1788080815723L, btc.timestamp());
        assertEquals("ETHUSDT", tickers.get(1).symbol());
    }

    private JsonNode fixture(String path) {
        return mapper.readTree(getClass().getResourceAsStream(path));
    }
}
