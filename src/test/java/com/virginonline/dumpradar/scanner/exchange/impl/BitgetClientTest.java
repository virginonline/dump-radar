package com.virginonline.dumpradar.scanner.exchange.impl;

import com.virginonline.dumpradar.config.ExchangeProperties;
import com.virginonline.dumpradar.config.RetryOn429Interceptor;
import com.virginonline.dumpradar.scanner.exchange.Exchange;
import com.virginonline.dumpradar.scanner.exchange.Timeframe;
import com.virginonline.dumpradar.scanner.model.Candle;
import com.virginonline.dumpradar.scanner.model.Ticker;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BitgetClientTest {

    private static final Instant NOW = Instant.parse("2026-09-03T16:00:05Z");

    private final MockWebServer server = new MockWebServer();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setUp() throws IOException {
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void candles_dropsFormingCandle() {
        server.enqueue(ok(candlesBody(
                ms("2026-09-03T04:00:00Z"),
                ms("2026-09-03T08:00:00Z"),
                ms("2026-09-03T12:00:00Z"),
                ms("2026-09-03T16:00:00Z"))));

        List<Candle> candles = client().candles("PEPEUSDT", Timeframe.H4, 3);

        assertEquals(3, candles.size());
        assertEquals(ms("2026-09-03T04:00:00Z"), candles.get(0).openTime());
        assertEquals(ms("2026-09-03T12:00:00Z"), candles.getLast().openTime());
    }

    @Test
    void candles_keepsJustClosed() {
        server.enqueue(ok(candlesBody(
                ms("2026-09-03T04:00:00Z"),
                ms("2026-09-03T08:00:00Z"),
                ms("2026-09-03T12:00:00Z"))));

        List<Candle> candles = client().candles("PEPEUSDT", Timeframe.H4, 3);

        assertEquals(3, candles.size());
        assertEquals(ms("2026-09-03T12:00:00Z"), candles.getLast().openTime());
    }

    @Test
    void candles_respectsLimit() throws InterruptedException {
        server.enqueue(ok(candlesBody(
                ms("2026-09-03T00:00:00Z"),
                ms("2026-09-03T04:00:00Z"),
                ms("2026-09-03T08:00:00Z"),
                ms("2026-09-03T12:00:00Z"))));

        List<Candle> candles = client().candles("PEPEUSDT", Timeframe.H4, 2);

        assertEquals(2, candles.size());
        assertEquals(ms("2026-09-03T00:00:00Z"), candles.get(0).openTime());
        assertEquals(ms("2026-09-03T04:00:00Z"), candles.getLast().openTime());

        RecordedRequest request = server.takeRequest();
        assertEquals("4H", request.getUrl().queryParameter("granularity"));
        assertEquals("3", request.getUrl().queryParameter("limit"));
    }

    @Test
    void retry_on429_then200() {
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(ok(tickersBody()));

        List<Ticker> tickers = client().tickers();

        assertEquals(1, tickers.size());
        assertEquals("PEPEUSDT", tickers.getFirst().symbol());
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void retry_exhausted_throws() {
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());

        assertThrows(RuntimeException.class, () -> client().tickers());

        assertEquals(3, server.getRequestCount());
    }

    private BitgetClient client() {
        return new BitgetClient(
                new ExchangeProperties(Map.of(Exchange.BITGET,
                        new ExchangeProperties.Endpoint(baseUrl()))),
                new OkHttpClient.Builder()
                        .addInterceptor(new RetryOn429Interceptor(2))
                        .build(),
                mapper,
                clock);
    }

    private String baseUrl() {
        String url = server.url("/").toString();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static MockResponse ok(String body) {
        return new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build();
    }

    private static MockResponse rateLimited() {
        return new MockResponse.Builder()
                .code(429)
                .addHeader("Retry-After", "0")
                .build();
    }

    private static String candlesBody(long... openTimes) {
        StringBuilder sb = new StringBuilder("{\"code\":\"00000\",\"msg\":\"success\",\"data\":[");
        for (int i = 0; i < openTimes.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("[\"").append(openTimes[i])
              .append("\",\"100\",\"140\",\"99\",\"134\",\"500\",\"6000\"]");
        }
        return sb.append("]}").toString();
    }

    private static String tickersBody() {
        return "{\"code\":\"00000\",\"msg\":\"success\",\"data\":[{"
                + "\"symbol\":\"PEPEUSDT\",\"lastPr\":\"0.0000142\",\"change24h\":\"0.42\","
                + "\"usdtVolume\":\"25000000\",\"ts\":\"1788450537000\"}]}";
    }

    private static long ms(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }
}
