package com.virginonline.dumpradar.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class HttpConfig {
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .addInterceptor(new RetryOn429Interceptor(2))
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
