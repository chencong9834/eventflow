# 压测报告

目标不是刷一个好看的 QPS，而是回答两个问题：**并发下库存会不会错**，以及**吞吐卡在哪里**。

## 环境

单机，所有组件跑在同一台开发机上，结论只在相对意义上有效（同一环境下的前后对比），不要当作生产容量。

| 项 | 配置 |
|---|---|
| 宿主 | Windows 10，Docker Desktop（Engine 29.8.0，WSL2 后端） |
| 应用 | JDK 17，`mvn spring-boot:run` 跑在宿主机，Tomcat 8080 |
| MySQL | 8.0，容器，`mem_limit: 512m`，`--default-time-zone=+00:00` |
| Redis / RabbitMQ | 7-alpine / 3.13-management，容器 |
| 连接池 | HikariCP，`maximum-pool-size: 32` |
| 压测机 | k6 容器，经 `host.docker.internal` 回打宿主机 |

脚本在 `scripts/k6/`：`order.js`（下单写路径）、`catalog.js`（目录读路径）、`baseline-http.js`（压测链路基准）。

## 先确认压测链路不是瓶颈

写路径只有 40 req/s 出头时，第一个要排除的怀疑对象是压测机和 Docker 的网络转发。用同一条链路打不碰数据库的 `/actuator/health`：

| 场景 | 并发 | 吞吐 | p95 |
|---|---|---|---|
| `/actuator/health` | 50 VU / 30s | **536 req/s** | 126 ms |

同一条链路能跑 536 req/s，所以后面测到的写路径上限是应用自己的，不是链路的。这一步不做的话，后面所有结论都站不住。

## 库存正确性：零超卖

库存 200，100 并发抢 30 秒，每人限购放到远大于库存（否则单个种子买家会先撞限购，测不出售罄）。压测脚本在 `teardown` 里回读库存，所以结论直接出现在输出里，不依赖事后手工查库。

| 指标 | 结果 |
|---|---|
| 下单成功 | **200**（精确等于库存） |
| 正确判为售罄 | 2784 |
| 支付成功 | 200 |
| 最终库存 | `total=200 available=0 reserved=0 sold=200` |
| 不变量 `available+reserved+sold==total` | 成立 |
| 失败检查 | 0 / 3184 |

3000 次并发请求抢 200 张票，多卖 0 张、少卖 0 张。这个结论在"库存更新移到事务末尾"的改动之后重跑过一次，仍然成立。

## 读路径

| 场景 | 并发 | 吞吐 | p95 | 失败率 |
|---|---|---|---|---|
| 目录列表 `/api/catalog/activities` | 50 VU / 60s | 344.7 req/s（合计） | 179 ms | 0% |
| 目录详情 `/api/catalog/activities/{id}` | 同上 | — | 258 ms | 0% |

## 写路径：定位瓶颈

下单一轮 = 一次下单 + 一次模拟支付，两个请求、两个独立事务。单票档、50 并发。

| # | 配置 | 下单吞吐 | 平均延迟 | 下单 p95 | 支付 p95 |
|---|---|---|---|---|---|
| 1 | 基线（连接池 10，无复合索引） | 21.6 单/s | 1.13 s | 1.61 s | 1.58 s |
| 2 | \+ 限购 SUM 复合索引 | 20.3 单/s | 1.19 s | 1.73 s | 1.68 s |
| 3 | \+ 连接池 10 → 32 | 20.4 单/s | 1.18 s | 1.46 s | 1.48 s |
| 4 | \+ 库存更新移到事务末尾 | **22.9 单/s** | **1.05 s** | 1.64 s | 1.65 s |
| 参照 | 同 #3，但写压力分散到 8 个票档 | **37.3 单/s** | **0.59 s** | 1.25 s | 1.18 s |

相同配置重复跑的吞吐波动约 ±5%，所以 #1 到 #3 之间的差异都在噪声内，下面的判断以此为准。

### 被否证的两个假设

**假设一：每人限购的 `SUM` 查询拖慢了下单。** `EXPLAIN` 确实显示它退化到外键索引 `fk_order_tier` 并扫描该票档下的全部订单行（压测时 1341 行）：

```
type=ref  key=fk_order_tier  rows=1341  Extra=Using where
```

于是加了覆盖索引 `(buyer_user_id, ticket_tier_id, status, qty)`（迁移 `V10`）。结果吞吐没有变化（#1 → #2 在噪声内）。索引本身是对的，留下了，但它不是瓶颈——扫 1341 行内存页的成本远小于这条路径上的其他开销。

**假设二：连接池太小。** 按 Little's law 估算，50 并发下每个请求平均占用连接约 250 ms，10 个连接理论上封顶约 40 req/s，和实测的 40.6 req/s 吻合得可疑。把池开到 32 后吞吐仍是 40.9 req/s（#2 → #3）。数字吻合是巧合，池并没有饱和。

### 真正的瓶颈：单行库存的行锁

把同样的压力分散到 8 个票档（8 行 `inventory`）后，吞吐从 20.4 涨到 37.3 单/s（**+83%**），平均延迟从 1.18 s 降到 0.59 s。除了写入的目标行不同，其他条件完全一致——所以差值就是**同一票档只有一行 `inventory`，扣减语句拿到该行排他锁并持有到事务提交，导致所有并发下单在这一行上串行化**。

这也解释了为什么前两个优化无效：瓶颈不在 CPU、索引或连接数上，而在一个逻辑上无法并行的临界区。

### 已做的优化：压缩临界区

原来的 `place()` 在扣库存之后还要插订单、写 outbox、写站内通知，然后才提交——行锁被白白持有了整个事务的长度。把库存扣减改成事务的最后一条语句后，临界区压缩到"一次条件更新 + 提交"：

```java
orderMapper.insert(order);
writeOutbox(ORDER_CREATED, order.getId().toString());
notice(...);
// 同一票档只有一行 inventory，扣减会拿到该行排他锁并持有到提交。
// 放在最后，把临界区压到"一次条件更新 + 提交"。
if (inventoryMapper.reserve(tier.getId(), qty) == 0) {
  throw new BizException(ErrorCode.SOLD_OUT, "库存不足", HttpStatus.CONFLICT);
}
```

影响行数为 0 时抛异常整笔回滚，前面的插入一并撤销，所以正确性不变——改完重跑了零超卖场景验证。`pay()` 里的 `confirmSold` 做了同样处理。

效果：吞吐 20.4 → 22.9 单/s（+12%），平均延迟 1.18 s → 1.05 s。提升幅度不大，因为提交时的 fsync 本来就在临界区内、去不掉；但这是零风险的改动。

## 下一步

按预期收益排序：

1. **库存分桶**：把一个票档的库存拆成 N 行，下单时按买家哈希或随机落到某一桶，售罄前不需要跨桶。上面的 8 票档参照实验已经量化了上限——同等条件下约 +83%。代价是"可售总量"要聚合 N 行，以及桶间余量碎片需要在接近售罄时合并。
2. **把 MySQL 扣减异步化**：Redis Lua 扣减成功即返回，落库由消息队列串行消费。吞吐由 Redis 决定，但"下单成功"变成最终一致，需要给用户可见的中间态。
3. **缩短提交开销**：当前临界区里剩下的主要成本是提交时的日志落盘。MySQL 参数调整不属于应用层优化，但在容量规划时要算进去。

前两条都会改变一致性模型或数据布局，属于契约变更，按 `PROJECT-CONTRACT.md` 的约定要先改文档。

## 复现

```bash
cd infra && docker compose up -d
mvn -pl backend spring-boot:run

# 链路基准
k6 run -e VUS=50 -e DURATION=30s scripts/k6/baseline-http.js

# 零超卖
k6 run -e VUS=100 -e DURATION=30s -e STOCK=200 -e PER_USER_LIMIT=1000000 scripts/k6/order.js

# 写路径吞吐
k6 run -e VUS=50 -e DURATION=60s -e STOCK=200000 scripts/k6/order.js

# 行锁对照
k6 run -e VUS=50 -e DURATION=30s -e STOCK=200000 -e TIERS=8 scripts/k6/order.js

# 读路径
k6 run -e VUS=50 -e DURATION=60s scripts/k6/catalog.js
```

k6 装在宿主机时 `BASE_URL` 用默认的 `http://localhost:8080`；用 k6 容器时加
`-e BASE_URL=http://host.docker.internal:8080` 并给 `docker run` 带上
`--add-host=host.docker.internal:host-gateway`。
