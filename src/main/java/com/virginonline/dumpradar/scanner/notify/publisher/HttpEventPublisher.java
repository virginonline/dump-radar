package com.virginonline.dumpradar.scanner.notify.publisher;

import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.notify.ChartFactory;
import com.virginonline.dumpradar.scanner.notify.EventPublisher;
import com.virginonline.dumpradar.scanner.notify.EventType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpEventPublisher implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(HttpEventPublisher.class);
  private static final MediaType JSON = MediaType.parse("application/json");
  private final ChartFactory chartFactory;
  private final OkHttpClient http;
  private final String url;

  public HttpEventPublisher(
      @Value("${app.notify.gateway-url:}") String gatewayUrl,
      ChartFactory chartFactory,
      OkHttpClient http) {
    this.chartFactory = chartFactory;
    this.http = http;
    this.url = gatewayUrl;
  }

  @Override
  public void publish(Candidate candidate, EventType type, Instant now, BigDecimal entry) {
    String payload;
    try {
      payload = chartFactory.compose(candidate, type, now, entry);
    } catch (Exception e) {
      log.error("failed to compose: {}", e.getMessage());
      return;
    }
    if (url == null || url.isBlank()) {
      log.info("gateway not configured, event dropped to log: {}", payload);
      return;
    }
    Request request =
        new Request.Builder().url(url).post(RequestBody.create(payload, JSON)).build();

    http.newCall(request)
        .enqueue(
            new okhttp3.Callback() {
              @Override
              public void onFailure(Call c, IOException e) {
                log.warn(
                    "notify delivery failed for {} {}: {}", type, candidate.id(), e.toString());
              }

              @Override
              public void onResponse(Call c, Response response) {
                try (response) {
                  if (!response.isSuccessful()) {
                    log.warn(
                        "notify rejected for {} {}: HTTP {}",
                        type,
                        candidate.id(),
                        response.code());
                  }
                }
              }
            });
  }
}
