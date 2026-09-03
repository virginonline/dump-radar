package com.virginonline.dumpradar.scanner.exchange.impl;

import com.virginonline.dumpradar.config.ExchangeProperties;
import com.virginonline.dumpradar.scanner.exchange.*;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BitgetClient extends AbstractExchangeClient implements MarketDataClient {

    private final Clock clock;
    private final TtlCache<Map<String, SymbolMeta>> symbolsCache;

    public BitgetClient(ExchangeProperties props, OkHttpClient http,
                        ObjectMapper mapper, Clock clock) {
        super(Exchange.BITGET, props.exchanges().get(Exchange.BITGET).baseUrl(), http, mapper);
        this.clock = clock;
        this.symbolsCache = new TtlCache<>(
                props.cache().ttl(),
                clock,
                this::fetchSymbolsRaw);
    }

    @Override
    public List<Ticker> tickers() {
        return fetch("/api/v2/mix/market/tickers?productType=USDT-FUTURES", Parser::parseTickers);
    }

    @Override
    public List<Candle> candles(String symbol, Timeframe timeframe, int limit) {
        List<Candle> raw = fetch("/api/v2/mix/market/candles?symbol=" + symbol
                + "&productType=USDT-FUTURES&granularity=" + timeframe.code()
                + "&limit=" + (limit + 1), Parser::parseCandles);
        if (!raw.isEmpty() && raw.getLast().openTime() + timeframe.duration().toMillis()
                > clock.instant().toEpochMilli()) {
            raw = raw.subList(0, raw.size() - 1);
        }
        return raw.size() > limit ? new ArrayList<>(raw.subList(0, limit)) : raw;
    }

    private Map<String, SymbolMeta> fetchSymbolsRaw() {
        return fetch("/api/v2/mix/market/contracts?productType=USDT-FUTURES", Parser::parseContracts)
                .stream()
                .collect(Collectors.toMap(SymbolMeta::symbol, m -> m));
    }

    @Override
    public Map<String, SymbolMeta> symbols() {
        return symbolsCache.get();
    }

    @Override
    public Exchange exchange() {
        return Exchange.BITGET;
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

        public static List<SymbolMeta> parseContracts(JsonNode data) {
            List<SymbolMeta> out = new ArrayList<>();
            for (JsonNode c : data) {
                out.add(new SymbolMeta(
                        c.get("symbol").asString(),
                        c.get("baseCoin").asString(),
                        0L,
                        c.get("symbolStatus").asString("")));
            }
            return out;
        }
    }
}
