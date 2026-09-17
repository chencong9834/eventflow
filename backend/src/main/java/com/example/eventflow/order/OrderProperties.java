package com.example.eventflow.order;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eventflow.order")
public class OrderProperties {

  private int payTimeoutSeconds = 900;

  public int getPayTimeoutSeconds() {
    return payTimeoutSeconds;
  }

  public void setPayTimeoutSeconds(int payTimeoutSeconds) {
    this.payTimeoutSeconds = payTimeoutSeconds;
  }
}
