package com.example.eventflow.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

  /** 测试用 @Primary 覆盖这个 Bean 来推进时间，例如验证支付超时关单。 */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
