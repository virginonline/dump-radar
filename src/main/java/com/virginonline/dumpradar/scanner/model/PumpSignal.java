package com.virginonline.dumpradar.scanner.model;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import java.time.Instant;

public record PumpSignal(
    String symbol,
    String baseAsset,
    Exchange exchange,
    Source source,
    Candle signalCandle,
    Instant detectedAt) {}
