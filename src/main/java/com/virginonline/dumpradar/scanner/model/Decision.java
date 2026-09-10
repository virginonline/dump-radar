package com.virginonline.dumpradar.scanner.model;

public record Decision(
    Candidate candidate, boolean created, boolean merged, boolean anchorUpdated) {}
