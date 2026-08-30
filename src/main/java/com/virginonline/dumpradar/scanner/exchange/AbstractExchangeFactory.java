package com.virginonline.dumpradar.scanner.exchange;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public abstract class AbstractExchangeFactory<T> {

    private final String baseUrl;
    private final ObjectMapper mapper;
    private final OkHttpClient http;

    public AbstractExchangeFactory(String baseUrl, OkHttpClient http, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.http = http;
    }

    protected abstract List<T> parse(JsonNode root);

    protected abstract String path();

    protected abstract String name();

    public List<T> fetch() {
        String url = this.baseUrl + path();
        Request request = new Request.Builder().url(url).get().build();
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
