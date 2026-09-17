# Project Contract

EventFlow 是活动售票 MVP：主办方发布活动与票档，购票用户下单支付后获得无座电子票，平台运营审核与监管。本文是实现的约束来源，定义功能边界与状态机；阶段性取舍见 [`DECISIONS.md`](DECISIONS.md)。

部署形态：一个 Spring Boot 模块化单体 + 独立前端。包与前端模块按领域划分（`tenant` / `identity` / `iam` / `activity` / `inventory` / `order` / `payment` / `ticket` / `refund` / `notification` / `reporting` / `audit` / `platform` / `shared` / `boot`），运行时仍是单一应用。

技术栈以根目录 `README.md` 为准：JDK 17、Spring Boot 3.x、Spring Security 6、MyBatis + Flyway、MySQL 8、Redis 7、RabbitMQ、JUnit 5 + Testcontainers；前端 React + TypeScript + Vite、TanStack Query、React Hook Form、Zod、Ant Design；Docker Compose + Nginx、Prometheus + Grafana + k6。

---

## 1. 角色

三类主体，均落在 `sys_user` 上。一个用户固定属于一个租户，登录后不能切租户。授权由 `iam` 的角色与权限表达，不做流程引擎。

### 1.1 平台运营（Platform）

- 归属内置平台租户（租户类型 `PLATFORM`）。
- 审核主办方租户入驻、活动/场次/票档上架。
- 查询跨租户经营事实（报表仍走事实表 + 时间范围，见第 5 节）。
- 处理风控与客服级整单退款（与主办方退款同一退款模型，仅权限不同）。
- 使用 `web/src/modules/platform` 与后端 `platform`。

### 1.2 主办方（Organizer）

- 归属自己的主办方租户（租户类型 `ORGANIZER`）。该租户下的活动、库存、订单、票券、退款、报表均带该 `tenant_id`。
- 维护活动、场次、票档；提交审核；查看本租户订单与核销；对本租户已支付且可退订单发起整单退款。
- 现场核销本租户票券（无座，一票一码）。
- 使用 `activity` / `inventory` / `order` / `ticket` / `refund` / `reporting` 的主办方界面。

### 1.3 购票用户（Buyer）

- 归属内置购票租户（租户类型 `BUYER`）。购票用户之间不按主办方拆租户。
- 浏览**已审核通过且在售**的活动（跨主办方目录是明确例外，只读已发布数据，不能改主办方数据）。
- 对单个场次的单个票档下单、模拟支付、查看自己的订单与票码；未支付可取消或等待关单。
- 使用 `web/src/modules/buyer` 以及购票相关的 `order` / `ticket` / `payment` 页面。

### 1.4 角色边界

| 允许 | 不允许 |
|---|---|
| 用户只携带登录时的 `tenant_id` | 登录后切租户、请求体传入并改写隔离用 `tenant_id` |
| 购票用户只读跨租户的已上架目录 | 购票用户写活动、库存、审核 |
| 主办方只读写本 `tenant_id` 业务数据 | 主办方看其他主办方订单/库存 |
| 平台审核与监管 | 平台冒充购票用户随意改库存而不走订单/退款用例 |

---

## 2. 核心实体

共享约定：

- 所有业务表包含 `tenant_id`（平台租户、主办方租户或购票租户，按数据归属写入，见各实体）。
- 金额：Java `Long` + 数据库整数，单位为**分**。禁止 `float` / `double` / 以元为单位的小数列。
- 时间：数据库与后端默认 UTC；API 输出带明确 UTC 含义；前端按用户时区展示。
- 主键使用雪花或 UUID（实现选定一种并全局一致）。审计字段：`created_at` / `updated_at` / `created_by`。

数据归属：

| 实体 | `tenant_id` 含义 |
|---|---|
| 租户、用户、角色 | 该账号所在租户 |
| 活动、场次、票档、库存、主办方侧订单/支付/票券/退款 | **主办方租户** |
| 购票用户账号、其站内通知偏好 | **购票租户** |
| 平台审核日志中的操作者 | 平台用户所在平台租户；被审对象仍指向主办方数据 |

购票用户下单后，订单行上同时记录 `buyer_user_id` 与主办方 `tenant_id`，以便两边查询且不切租户。

### 2.1 租户与账号（`tenant` / `identity` / `iam`）

- **Tenant**：`id`，`type`（`PLATFORM` \| `ORGANIZER` \| `BUYER`），`name`，`status`（启用/停用）。
- **User（`sys_user`）**：登录名/手机号、凭证摘要、`tenant_id`、状态。不支持多租户归属。
- **Role / Permission**：租户内角色；内置三角色模板（平台运营、主办方管理员、购票用户）。MVP 不自建复杂权限编排。

### 2.2 活动域（`activity`）

- **Activity（活动）**：标题、描述、封面、主办方 `tenant_id`，`review_status`，上架意图（草稿/提交审/已上架/已下架）。
- **Show（场次）**：所属活动、开演/结束 UTC、场次级售卖窗口、`review_status`（可与活动共用策略：场次随活动审，或票档变更需复审；MVP：**活动提交审核时锁定其下场次与票档快照式提交**，通过后改价改库存结构需重新提交审核）。
- **TicketTier（票档）**：所属场次（从而只有一个活动、一个场次）、名称、单价（分）、每人限购、`review_status`。无座位图、无分区选座。

一个场次可以有多个票档，供购票用户**选择其中一个**下单。一张订单不能混多个票档、不能跨场次。

### 2.3 库存（`inventory`）

- **Inventory**：按票档一行（或票档+场次，与票档 1:1）。字段：`total_qty`，`available_qty`，`reserved_qty`，`sold_qty`。
- 不变量：`available + reserved + sold == total`（作废/关单回补走 reserved→available 或 sold 按退款回补）。
- 开发期：MySQL 条件更新保证不超卖（`WHERE available_qty >= :qty`）。最终 MVP 可加 Redis Lua，仍以 MySQL 为对账真相。

### 2.4 订单（`order`）

- **Order**：`order_no`，主办方 `tenant_id`，`buyer_user_id`，`show_id`，`ticket_tier_id`，`qty`，`unit_price_fen`，`amount_fen`（`qty * unit_price_fen`，下单时快照），`status`，`pay_deadline_at`（UTC）。
- 一单一场次一票档。下单成功则占用 `reserved_qty`。

### 2.5 支付（`payment`）

- **Payment**：关联订单，`amount_fen`，`channel = SIMULATED`，`status`，`simulated_result`（成功/失败由调用方或测试接口指定）。
- 仅模拟支付：提供“确认支付成功 / 确认支付失败”的内部接口或页面按钮。不接微信、支付宝或其他三方。
- 同一订单支付意图必须幂等（Inbox / 业务唯一键：`order_id + payment_attempt` 或等价）。

### 2.6 票券（`ticket`）

- **Ticket**：一票一码。下单 `qty = N` 且支付成功后生成 N 条。无座位、无检票口分区。
- 字段：`ticket_no`（展示码，唯一）、`verify_code`（核销码，唯一，可与展示码同一）、`status`，所属订单/场次/票档，主办方 `tenant_id`，`buyer_user_id`。

### 2.7 退款（`refund`）

- **Refund**：整单退款单，金额等于订单 `amount_fen`。部分退、按张退不在 MVP。
- 模拟退款成功后回补库存、作废未核销票券、订单进入已退款。

### 2.8 审核与审计（`audit`）

- 业务对象上的 **`review_status`**：`DRAFT` / `PENDING` / `APPROVED` / `REJECTED`。
- **AuditOperationLog**：谁、何时、对何对象、从何状态到何状态、意见。不是流程引擎，无会签、无多级节点配置。

### 2.9 消息（`notification`）

- **Outbox**：业务事务内写事件，异步投递 RabbitMQ。
- **Inbox**：消费者按事件 ID 幂等。
- 用户可见通知（站内信即可）：支付成功、出票、关单、退款结果。

### 2.10 报表（`reporting`）

- 只读查询订单、支付、票券、退款事实表。必填时间窗。无独立数仓、无实时指标管道。

---

## 3. 状态机

未列出的迁移一律拒绝。状态值使用大写下划线，与 API 一致。

### 3.1 审核 `review_status`（活动提交集：活动 + 其下场次 + 票档）

```
DRAFT --提交--> PENDING --通过--> APPROVED
                   |                |
                   +--驳回--> REJECTED --修改后重提--> PENDING
APPROVED --主办方下架--> 售卖关闭（活动/场次售卖开关，不回退为 DRAFT）
APPROVED --改价/改票档结构--> 须重新 PENDING（已售出订单仍以订单快照为准）
```

- 仅 `APPROVED` 且在售卖窗口内的票档可被购票用户下单。
- 平台写 `audit_operation_log`；主办方提交/重提也写日志。

### 3.2 库存（数量，非枚举）

| 动作 | 变化 |
|---|---|
| 下单成功 | `available -= qty`，`reserved += qty` |
| 支付成功出票 | `reserved -= qty`，`sold += qty` |
| 关单 / 用户取消待支付 / 模拟支付失败且释放 | `reserved -= qty`，`available += qty` |
| 整单退款成功 | `sold -= qty`，`available += qty` |

任何一步条件更新失败则整笔业务失败，禁止出现负数。

### 3.3 订单 `Order.status`

```
CREATED --支付成功--> PAID --发起整单退款且完成--> REFUNDED
   |                    |
   +--超时关单/买家取消/模拟支付失败释放--> CANCELLED
PAID --全部票券核销完成--> FULFILLED（可选汇总状态，票券仍以票为准）
```

- `CREATED`：已占预留库存，等待模拟支付，存在 `pay_deadline_at`。
- `PAID`：已生成票券。
- `CANCELLED`：未支付关闭，库存已回补。
- `REFUNDED`：整单退款完成。
- `FULFILLED`：该单全部票券 `USED`（便于报表；若不想多一个状态，可用票券聚合推导，实现时二选一并保持报表一致）。**选定：保留 `FULFILLED`，在最后一张票核销成功时写入。**

不允许：`PAID` → `CANCELLED`；部分支付；一单两次成功支付。

### 3.4 关单

- 下单事务提交后写 Outbox 延迟消息（Rabbit 延迟消息或延迟插件），到期尝试关单。
- 定时扫描 `CREATED AND pay_deadline_at < now` 作为兜底。
- 关单必须条件更新订单仍为 `CREATED`，成功才回补库存。已支付的订单忽略关单消息（幂等）。

### 3.5 支付 `Payment.status`

```
PENDING --模拟成功--> SUCCEEDED
   |
   +--模拟失败--> FAILED
```

- 仅 `SUCCEEDED` 可驱动订单 `PAID` 与出票。
- 重复投递同一成功事件：Inbox 去重，订单保持 `PAID`。

### 3.6 票券 `Ticket.status`

```
UNUSED --核销成功--> USED
UNUSED --整单退款--> VOID
```

- 核销：主办方本租户、场次有效、票码正确、状态 `UNUSED`。重复核销拒绝。
- 已有任意一张 `USED` 的订单：**不可整单退款**（避免已入场票回补库存）。全部 `UNUSED` 才允许退。

### 3.7 退款 `Refund.status`

```
REQUESTED --模拟退款成功--> SUCCEEDED
     |
     +--拒绝/失败--> REJECTED
```

- 仅订单 `PAID` 且全部票 `UNUSED` 可 `REQUESTED`。
- `SUCCEEDED` 时：订单 `REFUNDED`，票 `VOID`，库存按第 3.2 节回补。
- MVP 不做部分退、不做改期。

### 3.8 租户与用户

- Tenant：`ACTIVE` / `DISABLED`（停用后该租户用户不可登录，已产生订单按原状态机收尾，不再新下单）。
- User：`ACTIVE` / `DISABLED`。

---

## 4. 领域事件（Outbox）

事务内落库，再投 Rabbit。消费者 Inbox 幂等。

| 事件 | 何时 | 主要消费者 |
|---|---|---|
| `OrderCreated` | 下单占库存成功 | 延迟关单 |
| `OrderCancelled` | 关单或取消 | 通知 |
| `PaymentSucceeded` | 模拟支付成功 | 出票、通知 |
| `TicketsIssued` | 票券写入完成 | 通知购票用户 |
| `OrderFulfilled` | 末张票核销 | 报表/通知可选 |
| `RefundSucceeded` | 整单退款完成 | 通知、库存已在同一事务处理则消费者只通知 |
| `ReviewDecided` | 审核通过或驳回 | 通知主办方 |

库存变更与订单状态变更必须在**同一本地事务**（订单/库存/Outbox），不能只靠异步消息扣减库存。

---

## 5. 非功能

| 项 | 约定 |
|---|---|
| 隔离 | 共享库共享表 + `tenant_id`。写路径 `tenant_id` 只来自登录上下文（购票下单写入**活动所属主办方租户**，不是购票用户租户）。读路径默认过滤；购票目录、平台监管为白名单例外。 |
| 金额 | 分，`Long`。展示层格式化为元，不得回写浮点。 |
| 时间 | 存储 UTC。关单、开售、开演均用 UTC 比较。 |
| 一致性 | 下单、支付出票、关单、退款：MySQL 事务 + 条件更新。消息最终一致，消费幂等。 |
| 并发 | 先 MySQL 正确性（含并发下单测试）。再可选 Redis Lua 预扣，对账以 MySQL 为准。 |
| 关单 | 延迟消息 + DB 扫描，二者都要幂等。 |
| 安全 | 认证后鉴权；核销接口仅主办方；票码不可枚举猜测（足够熵）；审计日志不可用户篡改。细节见 `docs/SECURITY.md`。 |
| API | 统一响应与错误码，见 `docs/API-CONVENTIONS.md`。 |
| 可运维 | 单体 + compose（MySQL / Redis / RabbitMQ）。健康检查。报表查询必须带时间范围，防止全表扫。 |
| 可测 | 超卖、重复支付、重复核销、跨租户读写、关单与已支付竞态，均需自动化测试。 |

---

## 6. MVP 边界

### 6.1 做

- 三角色与固定租户登录；平台审核（`review_status` + `audit_operation_log`）。
- 活动 / 场次 / 多票档维护；无座；按票档库存。
- 购票：选一个场次的一个票档、数量、下单、模拟支付、出票、查看票码。
- 关单（延迟消息 + 扫描）；整单退款（未核销）。
- 主办方核销；本租户订单与简单报表（时间范围内的销量、销售额分、退款额分）。
- Outbox + RabbitMQ + Inbox；站内通知。
- 库存 MySQL 基线；文档与测试预留 Lua 切换点。

### 6.2 不做

- 微信、支付宝及任何真实收单、分账、对账单。
- 选座、座位图、有座票、打印纸质票物流。
- 一单多票档、一单多场次、购物车、拼团、优惠券、积分。
- 登录后切租户、一个账号多租户、用户自选 `tenant_id`。
- 部分退款、按张退、改期换票。
- 工作流引擎、多级可配置审批流。
- 实时数仓、离线 ETL、复杂 OLAP。
- 微服务拆分、分库分表、多活。
- 渠道分销、代理商、票牛式转卖。
- 会员体系、内容社区、直播。

### 6.3 开发顺序约束

1. 共享内核（租户注入、金额时间、Outbox 表）。
2. 账号与角色。
3. 活动审核与目录。
4. MySQL 库存 + 下单 + 关单。
5. 模拟支付 + 出票。
6. 核销。
7. 整单退款。
8. 通知与报表。
9. 需要时再上 Redis Lua，不得削弱 MySQL 不变量。

---

## 7. 模块对照

| 目录 | 职责 |
|---|---|
| `boot` | 应用启动、配置 |
| `shared` | 响应体、租户上下文、金额/时间、Outbox 基础设施 |
| `tenant` / `identity` / `iam` | 租户、用户、角色权限 |
| `activity` | 活动、场次、票档、提交审核 |
| `inventory` | 库存数量与条件更新 |
| `order` | 下单、取消、关单 |
| `payment` | 模拟支付 |
| `ticket` | 出票、票码、核销 |
| `refund` | 整单退款 |
| `notification` | 投递与站内信 |
| `reporting` | 事实表查询 |
| `audit` | `audit_operation_log` |
| `platform` | 平台审核与跨租户只读监管 |
| `web/src/auth` `layouts` `tenant` | 登录与壳 |
| `web/src/modules/*` | 与后端领域对应的界面 |

实现不得突破第 6 节边界；新增能力先改本契约再改代码。
