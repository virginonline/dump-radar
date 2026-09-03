package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.PrefilterProperties;
import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import com.virginonline.dumpradar.scanner.model.Ticker;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TickerPrefilter {

    public List<Ticker> filter(List<Ticker> tickers, Map<String, SymbolMeta> symbols,
                               PrefilterProperties props, Instant now) {
        List<Ticker> out = new ArrayList<>();
        for (Ticker ticker : tickers) {
            SymbolMeta meta = symbols.get(ticker.symbol());
            if (meta == null) continue;
            if (!isAlive(meta.symbolStatus())) continue;
            if (volumeTooLow(ticker, props)) continue;
            if (ticker.last().compareTo(props.minPrice()) < 0) continue;
            if (!oldEnough(meta.firstSeen(), now, props.minListingAgeDays())) continue;
            if (props.deniedBaseCoins().contains(meta.baseCoin())) continue;
            out.add(ticker);
        }
        return out;
    }

    private boolean isAlive(String status) {
        return "normal".equals(status);
    }

    private boolean volumeTooLow(Ticker ticker, PrefilterProperties props) {
        return ticker.volumeQuote().compareTo(props.minVolume24h()) < 0;
    }

    private boolean oldEnough(long firstSeen, Instant now, int minDays) {
        return firstSeen == 0L
                || now.toEpochMilli() - firstSeen >= minDays * 86_400_000L;
    }
}
