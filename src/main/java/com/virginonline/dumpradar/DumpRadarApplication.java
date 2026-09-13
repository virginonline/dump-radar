package com.virginonline.dumpradar;

import com.virginonline.dumpradar.config.props.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(
    value = {
      ConfirmProperties.class,
      PoolProperties.class,
      ExchangeProperties.class,
      ScreenProperties.class,
      PrefilterProperties.class,
      Scanner15mProperties.class,
    })
public class DumpRadarApplication {

  public static void main(String[] args) {
    SpringApplication.run(DumpRadarApplication.class, args);
  }
}
