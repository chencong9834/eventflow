package com.example.eventflow.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eventflow.auth")
public class AuthProperties {

  private int loginMaxFailures = 5;
  private int loginWindowSeconds = 900;
  private int loginMaxAttemptsPerIp = 30;

  public int getLoginMaxFailures() {
    return loginMaxFailures;
  }

  public void setLoginMaxFailures(int loginMaxFailures) {
    this.loginMaxFailures = loginMaxFailures;
  }

  public int getLoginWindowSeconds() {
    return loginWindowSeconds;
  }

  public void setLoginWindowSeconds(int loginWindowSeconds) {
    this.loginWindowSeconds = loginWindowSeconds;
  }

  public int getLoginMaxAttemptsPerIp() {
    return loginMaxAttemptsPerIp;
  }

  public void setLoginMaxAttemptsPerIp(int loginMaxAttemptsPerIp) {
    this.loginMaxAttemptsPerIp = loginMaxAttemptsPerIp;
  }
}
