package com.example.eventflow.shared.id;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdConfig {

  /** 数据中心号来自网卡，机器号来自数据中心号和进程号。 */
  @Bean
  public Snowflake snowflake() {
    return IdUtil.getSnowflake();
  }
}
