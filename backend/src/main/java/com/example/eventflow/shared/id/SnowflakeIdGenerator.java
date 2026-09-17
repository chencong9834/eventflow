package com.example.eventflow.shared.id;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

  private static final long EPOCH = 1704067200000L;
  private static final long WORKER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;
  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
  private static final long WORKER_SHIFT = SEQUENCE_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

  private final long workerId;
  private long sequence = 0L;
  private long lastTs = -1L;

  public SnowflakeIdGenerator() {
    this(1L);
  }

  public SnowflakeIdGenerator(long workerId) {
    if (workerId > MAX_WORKER_ID || workerId < 0) {
      throw new IllegalArgumentException("workerId out of range");
    }
    this.workerId = workerId;
  }

  public synchronized long nextId() {
    long ts = System.currentTimeMillis();
    if (ts < lastTs) {
      throw new IllegalStateException("clock moved backwards");
    }
    if (ts == lastTs) {
      sequence = (sequence + 1) & ~(-1L << SEQUENCE_BITS);
      if (sequence == 0) {
        while (ts <= lastTs) {
          ts = System.currentTimeMillis();
        }
      }
    } else {
      sequence = 0L;
    }
    lastTs = ts;
    return ((ts - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_SHIFT) | sequence;
  }
}
