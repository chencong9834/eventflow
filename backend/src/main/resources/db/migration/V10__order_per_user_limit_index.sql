-- 每人限购校验（TicketOrderMapper.sumActiveQty）在每次下单时执行：
--   SELECT SUM(qty) FROM ticket_order
--   WHERE buyer_user_id = ? AND ticket_tier_id = ? AND status IN ('CREATED','PAID','FULFILLED')
--
-- 已有索引都覆盖不到这个组合：idx_order_buyer 是 (buyer_user_id, created_at)，
-- 第二列用不上；外键索引 fk_order_tier 只有 ticket_tier_id 单列。压测中 EXPLAIN 显示
-- 优化器退化到 fk_order_tier 并扫描该票档下的全部订单行。
--
-- 下面的复合索引让前两列走等值、status 走范围，并把 qty 放进索引做覆盖查询，
-- 避免回表。

CREATE INDEX idx_order_buyer_tier_status
  ON ticket_order (buyer_user_id, ticket_tier_id, status, qty);
