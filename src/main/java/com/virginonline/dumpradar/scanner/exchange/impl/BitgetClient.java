package com.virginonline.dumpradar.scanner.exchange.impl;

import com.virginonline.dumpradar.config.ExchangeProperties;
import com.virginonline.dumpradar.scanner.exchange.AbstractExchangeClient;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Ticker;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class BitgetClient extends AbstractExchangeClient {

    public BitgetClient(ExchangeProperties props, OkHttpClient http, ObjectMapper mapper) {
        super(props.exchanges().get(Exchange.BITGET).baseUrl(), http, mapper);
    }

    public List<Ticker> fetchTickers() {
        return fetch("/api/v2/mix/market/tickers?productType=USDT-FUTURES", Parser::parseTickers);
    }

    public List<Candle> fetchCandles(String symbol, String granularity, int limit) {
        return fetch("/api/v2/mix/market/candles?symbol=" + symbol
                + "&productType=USDT-FUTURES&granularity=" + granularity
                + "&limit=" + limit, Parser::parseCandles);
    }

    @Override
    protected String name() {
        return Exchange.BITGET.name();
    }


    static class Parser {
        public static List<Candle> parseCandles(JsonNode data) {
            List<Candle> out = new ArrayList<>();
            for (JsonNode row : data) {
                out.add(new Candle(
                        Long.parseLong(row.get(0).asString()),
                        new BigDecimal(row.get(1).asString()),
                        new BigDecimal(row.get(2).asString()),
                        new BigDecimal(row.get(3).asString()),
                        new BigDecimal(row.get(4).asString()),
                        new BigDecimal(row.get(6).asString())));
            }
            return out;
        }

        public static List<Ticker> parseTickers(JsonNode data) {
            List<Ticker> out = new ArrayList<>();
            for (JsonNode t : data) {
                out.add(
                        new Ticker(
                                t.get("symbol").asString(),
                                new BigDecimal(t.get("lastPr").asString()),
                                t.get("change24h").asDouble(),
                                new BigDecimal(t.get("usdtVolume").asString()),
                                Long.parseLong(t.get("ts").asString())
                        )
                );
            }
            return out;
        }
    }
}
