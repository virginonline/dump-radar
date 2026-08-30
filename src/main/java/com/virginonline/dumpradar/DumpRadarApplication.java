package com.virginonline.dumpradar;

import com.virginonline.dumpradar.config.ExchangeProperties;
import com.virginonline.dumpradar.config.ScreenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(value = {ExchangeProperties.class, ScreenProperties.class})
public class DumpRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(DumpRadarApplication.class, args);
    }

}
