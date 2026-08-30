package com.virginonline.scanner.exchange;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public abstract class AbstractExchangeFactory<T> {

    protected final OkHttpClient http = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(10))
        .build();

    protected final ObjectMapper mapper = new ObjectMapper();

    protected abstract String baseUrl();

    protected abstract List<T> parse(JsonNode root);

    protected abstract String name();

    public List<T> fetch() {
        Request request = new Request.Builder().url(baseUrl()).get().build();
        try (Response response = http.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new IOException(name() + " rate limited (429)");
            }
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException(name() + " HTTP " + response.code());
            }
            return parse(mapper.readTree(response.body().string()));
        } catch (IOException e) {
            throw new RuntimeException(name() + " request failed", e);
        }
    }
}
