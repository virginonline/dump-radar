package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.scanner.model.SymbolMeta;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ListingAgeTracker {
    ConcurrentHashMap<String, Long> firstSeen = new ConcurrentHashMap<>();

    public Map<String, SymbolMeta> enrich(Map<String, SymbolMeta> fresh, Instant now) {
        if (firstSeen.isEmpty()) {
            fresh.keySet().forEach(k -> firstSeen.put(k, 0L));
        }
        fresh.keySet().forEach(k -> firstSeen.putIfAbsent(k, now.toEpochMilli()));
        Map<String, SymbolMeta> enriched = new ConcurrentHashMap<>();
        fresh.forEach((symbol, meta) -> enriched.put(symbol,
                new SymbolMeta(meta.symbol(), meta.baseCoin(),
                        firstSeen.get(symbol), meta.symbolStatus())));
        return enriched;
    }
}
