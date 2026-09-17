package com.example.eventflow.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class Utc {

  private Utc() {}

  /** 当前 UTC 挂钟时间。业务代码必须走这里，不能用 LocalDateTime.now()，后者取 JVM 默认时区。 */
  public static LocalDateTime now(Clock clock) {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  public static LocalDateTime from(Instant instant) {
    if (instant == null) {
      return null;
    }
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  public static Instant toInstant(LocalDateTime time) {
    if (time == null) {
      return null;
    }
    return time.toInstant(ZoneOffset.UTC);
  }
}
