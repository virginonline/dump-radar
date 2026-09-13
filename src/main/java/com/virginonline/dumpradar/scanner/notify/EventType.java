package com.virginonline.dumpradar.scanner.notify;

public enum EventType {
  NEW("candidate.new"),
  ANCHOR_UPDATE("candidate.anchor_update"),
  CONFIRMED("candidate.confirmed"),
  EXPIRED("candidate.expired"),
  MISSED("candidate.missed");

  private final String wireName;

  EventType(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }
}
