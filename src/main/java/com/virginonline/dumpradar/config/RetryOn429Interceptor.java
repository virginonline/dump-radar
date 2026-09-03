package com.virginonline.dumpradar.config;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

public class RetryOn429Interceptor implements Interceptor {
    private final int maxRetries;

    public RetryOn429Interceptor(int maxRetries) {
        this.maxRetries = maxRetries;
    }


    @Override
    public @NonNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        int attempts = 0;
        while (response.code() == 429 && attempts < maxRetries) {
            attempts++;
            long waitMillis = parseRetryAfter(response)
                    .orElse((500L << attempts) + ThreadLocalRandom.current().nextLong(250));
            response.close();
            try {
                Thread.sleep(waitMillis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during 429 backoff", e);
            }
            response = chain.proceed(request);
        }
        return response;
    }


    private OptionalLong parseRetryAfter(Response response) {
        String header = response.header("Retry-After");
        if (header == null) return OptionalLong.empty();
        try {
            return OptionalLong.of(Math.min(Long.parseLong(header) * 1000L, 15_000L));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }
}
