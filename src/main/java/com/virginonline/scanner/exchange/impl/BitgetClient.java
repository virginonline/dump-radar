package com.virginonline.scanner.exchange.impl;

import com.virginonline.scanner.exchange.AbstractExchangeFactory;
import com.virginonline.scanner.model.Ticker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

public class BitgetClient extends AbstractExchangeFactory<Ticker> {

    @Override
    protected String baseUrl() {
        return "https://api.bitget.com/api/v2/mix/market/tickers?productType=USDT-FUTURES";
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
    protected String name() {
        return "Bitget";
    }
}
