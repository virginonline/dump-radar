package com.virginonline.dumpradar.scanner.model;

import java.time.Instant;

public record Confirmation(ConfirmationKind kind, Candle candle, Instant at) {}
