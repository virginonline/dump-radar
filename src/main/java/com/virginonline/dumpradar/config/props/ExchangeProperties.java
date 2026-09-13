package com.virginonline.dumpradar.config.props;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ExchangeProperties(Cache cache, Map<Exchange, Endpoint> exchanges) {
  public record Cache(Duration ttl) {}

  public record Endpoint(String baseUrl) {}
}
