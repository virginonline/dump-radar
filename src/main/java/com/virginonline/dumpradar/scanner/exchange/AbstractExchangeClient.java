package com.virginonline.dumpradar.scanner.exchange;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractExchangeClient {

    private final String baseUrl;
    private final ObjectMapper mapper;
    private final OkHttpClient http;

    public AbstractExchangeClient(String baseUrl, OkHttpClient http, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.http = http;
    }


    protected abstract String name();

    public <T> List<T> fetch(String path, Function<JsonNode, List<T>> parser) {
        Request request = new Request.Builder().url(baseUrl + path).get().build();
        try (Response response = http.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new IOException(name() + " rate limited (429)");
            }
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException(name() + " HTTP " + response.code());
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                throw new IOException(name() + " API error "
                        + root.path("code").asString("") + ": " + root.path("msg").asString(""));
            }
            return parser.apply(data);
        } catch (IOException e) {
            throw new RuntimeException(name() + " request failed", e);
        }
    }
}
