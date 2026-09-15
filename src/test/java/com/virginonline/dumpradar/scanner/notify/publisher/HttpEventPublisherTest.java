package com.virginonline.dumpradar.scanner.notify.publisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.model.Candidate;
import com.virginonline.dumpradar.scanner.model.CandidateState;
import com.virginonline.dumpradar.scanner.model.Source;
import com.virginonline.dumpradar.scanner.notify.ChartFactory;
import com.virginonline.dumpradar.scanner.notify.EventType;
import com.virginonline.dumpradar.testfix.FakeRecorder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class HttpEventPublisherTest {

  private static final Instant T = Instant.parse("2026-09-14T12:00:00Z");

  private final ObjectMapper mapper = new ObjectMapper();
  private final MockWebServer server = new MockWebServer();

  @BeforeEach
  void setUp() throws Exception {
    server.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.close();
  }

  @Test
  void postsNotifyJsonToGateway() throws Exception {
    server.enqueue(new MockResponse.Builder().code(200).build());
    HttpEventPublisher publisher =
        new HttpEventPublisher(server.url("/notify").toString(), factory(), new OkHttpClient());

    publisher.publish(candidate(), EventType.NEW, T, null);

    RecordedRequest request = server.takeRequest(2, java.util.concurrent.TimeUnit.SECONDS);
    assertEquals("/notify", request.getUrl().encodedPath());
    assertTrue(request.getHeaders().get("Content-Type").startsWith("application/json"));

    JsonNode body = mapper.readTree(request.getBody().utf8());
    assertEquals("dumpbot.notify/v1", body.get("schema").asString());
    assertEquals("candidate.new", body.get("event").asString());
    assertEquals("PEPEUSDT-BITGET-20260914T120000Z", body.get("candidateId").asString());
  }

  @Test
  void rejectedByGateway_doesNotThrow() {
    server.enqueue(new MockResponse.Builder().code(500).build());
    HttpEventPublisher publisher =
        new HttpEventPublisher(server.url("/notify").toString(), factory(), new OkHttpClient());

    assertDoesNotThrow(() -> publisher.publish(candidate(), EventType.NEW, T, null));
  }

  @Test
  void blankUrl_dropsToLogWithoutHttp() {
    HttpEventPublisher publisher = new HttpEventPublisher("", factory(), new OkHttpClient());

    assertDoesNotThrow(() -> publisher.publish(candidate(), EventType.NEW, T, null));
    assertEquals(0, server.getRequestCount());
  }

  private ChartFactory factory() {
    return new ChartFactory("dump-radar", new FakeRecorder(), mapper, List.of());
  }

  private Candidate candidate() {
    return new Candidate(
        "PEPEUSDT-BITGET-20260914T120000Z",
        "PEPE",
        "PEPEUSDT",
        Set.of(Exchange.BITGET),
        Source.SCAN_4H,
        CandidateState.WATCHING,
        new BigDecimal("142"),
        new BigDecimal("100"),
        T,
        T.plusSeconds(86_400),
        null,
        T);
  }
}
