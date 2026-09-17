package com.example.eventflow.identity;

import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.security.AuthProperties;
import java.time.Duration;
import java.util.Locale;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

  private final StringRedisTemplate redis;
  private final AuthProperties properties;

  public LoginRateLimiter(StringRedisTemplate redis, AuthProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  public void assertAllowed(String username, String ip) {
    if (atLeast(userKey(username, ip), properties.getLoginMaxFailures())) {
      throw tooMany();
    }
  }

  public void recordAttempt(String ip) {
    Long count = increment(ipKey(ip));
    if (count != null && count > properties.getLoginMaxAttemptsPerIp()) {
      throw tooMany();
    }
  }

  public void recordFailure(String username, String ip) {
    increment(userKey(username, ip));
  }

  public void clearFailures(String username, String ip) {
    redis.delete(userKey(username, ip));
  }

  private boolean atLeast(String key, int max) {
    String value = redis.opsForValue().get(key);
    return value != null && Long.parseLong(value) >= max;
  }

  private Long increment(String key) {
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, Duration.ofSeconds(properties.getLoginWindowSeconds()));
    }
    return count;
  }

  private static String ipKey(String ip) {
    return "eventflow:login:ip:" + ip;
  }

  private static String userKey(String username, String ip) {
    return "eventflow:login:fail:" + ip + ":" + username.toLowerCase(Locale.ROOT);
  }

  private static BizException tooMany() {
    return new BizException(ErrorCode.LOGIN_RATE_LIMITED, "登录尝试过多，请稍后再试", HttpStatus.TOO_MANY_REQUESTS);
  }
}
