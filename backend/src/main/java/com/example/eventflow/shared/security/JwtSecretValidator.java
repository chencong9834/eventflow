package com.example.eventflow.shared.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretValidator implements InitializingBean {

  static final String DEV_DEFAULT_MARKER = "local-dev-only";

  private final Environment environment;
  private final JwtProperties jwtProperties;

  public JwtSecretValidator(Environment environment, JwtProperties jwtProperties) {
    this.environment = environment;
    this.jwtProperties = jwtProperties;
  }

  @Override
  public void afterPropertiesSet() {
    String secret = jwtProperties.getSecret();
    if (secret == null || secret.getBytes().length < 32) {
      throw new IllegalStateException("eventflow.jwt.secret must be at least 32 bytes");
    }
    if (environment.matchesProfiles("local", "test")) {
      return;
    }
    if (secret.contains(DEV_DEFAULT_MARKER)) {
      throw new IllegalStateException("EVENTFLOW_JWT_SECRET is required outside local/test profiles");
    }
  }
}
