package com.virginonline.dumpradar.scanner.exchange.impl;

import com.virginonline.dumpradar.config.ExchangeProperties;
import com.virginonline.dumpradar.scanner.exchange.AbstractExchangeFactory;
import com.virginonline.dumpradar.scanner.model.Ticker;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class BitgetClient extends AbstractExchangeFactory<Ticker> {

    public BitgetClient(ExchangeProperties props, OkHttpClient http, ObjectMapper mapper) {
        super(props.exchanges().get("bitget").baseUrl(), http, mapper);
    }

    @Override
    protected List<Ticker> parse(JsonNode root) {
        List<Ticker> out = new ArrayList<>();
        long now = Instant.now().toEpochMilli();
        for (JsonNode t : root.get("data")) {
            out.add(
                    new Ticker(
                            t.get("symbol").asString(),
                            new BigDecimal(t.get("lastPr").asString()),
                            t.get("change24h").asDouble(),
                            new BigDecimal(t.get("usdtVolume").asString()),
                            now
                    )
            );
        }
        return out;
    }

    @Override
    protected String path() {
        return "/api/v2/mix/market/tickers?productType=USDT-FUTURES";
    }

    @Override
    protected String name() {
        return "Bitget";
    }
}
