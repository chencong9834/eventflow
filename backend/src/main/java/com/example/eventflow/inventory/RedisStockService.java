package com.example.eventflow.inventory;

import java.util.List;
import java.util.function.IntSupplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Redis 可售库存。Lua 先扣减，MySQL 事务成功才保留；回滚时把数量加回去。
 */
@Component
public class RedisStockService {

  static final String KEY_PREFIX = "eventflow:inv:avail:";

  private static final String RESERVE_LUA =
      """
      local current = redis.call('GET', KEYS[1])
      if not current then
        return -1
      end
      local qty = tonumber(ARGV[1])
      current = tonumber(current)
      if current < qty then
        return 0
      end
      redis.call('DECRBY', KEYS[1], qty)
      return 1
      """;

  private static final String RESTORE_LUA =
      """
      if redis.call('EXISTS', KEYS[1]) == 1 then
        redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
      end
      return 1
      """;

  private final StringRedisTemplate redis;
  private final RedisScript<Long> reserveScript;
  private final RedisScript<Long> restoreScript;

  public RedisStockService(StringRedisTemplate redis) {
    this.redis = redis;
    this.reserveScript = script(RESERVE_LUA);
    this.restoreScript = script(RESTORE_LUA);
  }

  public void replaceAvailable(Long ticketTierId, int availableQty) {
    redis.opsForValue().set(key(ticketTierId), Integer.toString(Math.max(0, availableQty)));
  }

  public void delete(Long ticketTierId) {
    redis.delete(key(ticketTierId));
  }

  public boolean tryReserve(Long ticketTierId, int qty, IntSupplier loadAvailable) {
    for (int attempt = 0; attempt < 8; attempt++) {
      Long result = redis.execute(reserveScript, List.of(key(ticketTierId)), String.valueOf(qty));
      if (result != null && result.longValue() == 1L) {
        restoreIfRollback(ticketTierId, qty);
        return true;
      }
      if (result != null && result.longValue() == 0L) {
        return false;
      }
      int loaded = Math.max(0, loadAvailable.getAsInt());
      redis.opsForValue().setIfAbsent(key(ticketTierId), Integer.toString(loaded));
    }
    return false;
  }

  public void restoreAfterCommit(Long ticketTierId, int qty) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      incrIfPresent(ticketTierId, qty);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            incrIfPresent(ticketTierId, qty);
          }
        });
  }

  private void restoreIfRollback(Long ticketTierId, int qty) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              incrIfPresent(ticketTierId, qty);
            }
          }
        });
  }

  private void incrIfPresent(Long ticketTierId, int qty) {
    redis.execute(restoreScript, List.of(key(ticketTierId)), String.valueOf(qty));
  }

  static String key(Long ticketTierId) {
    return KEY_PREFIX + ticketTierId;
  }

  private static RedisScript<Long> script(String lua) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(lua);
    script.setResultType(Long.class);
    return script;
  }
}
