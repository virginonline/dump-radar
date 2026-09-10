package com.virginonline.dumpradar.scanner.model;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record Candidate(
    String id,
    String baseAsset,
    Set<Exchange> exchanges,
    Source source,
    CandidateState state,
    BigDecimal anchorHigh,
    BigDecimal pumpStart,
    Instant detectedAt,
    Instant deadline,
    Instant confirmedAt,
    Instant updatedAt) {}
