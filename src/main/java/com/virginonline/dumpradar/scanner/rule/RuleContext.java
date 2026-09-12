package com.virginonline.dumpradar.scanner.rule;

import com.virginonline.dumpradar.scanner.model.Candidate;
import java.math.BigDecimal;

public record RuleContext(Candidate candidate, BigDecimal avgVolume) {}
