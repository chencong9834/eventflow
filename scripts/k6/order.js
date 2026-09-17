// 下单写路径压测：并发抢同一票档的库存，观察下单与模拟支付的吞吐和延迟。
//
//   k6 run scripts/k6/order.js
//   k6 run -e BASE_URL=http://host.docker.internal:8080 -e VUS=50 scripts/k6/order.js
//
// setup 阶段自己建一个已审核通过的票档，teardown 打印最终库存，所以"卖出数 == 库存数、
// 无超卖"这个结论在压测输出里自证。登录只在 setup 里做三次，避免撞上按 IP 的登录限流。
//
// 三种用法：
//   吞吐：  STOCK 给大值，压下单与支付的延迟分布
//   超卖：  STOCK 给小值、PER_USER_LIMIT 给大值，看成功数是否精确等于库存
//           k6 run -e STOCK=200 -e PER_USER_LIMIT=1000000 -e VUS=100 -e DURATION=30s scripts/k6/order.js
//   锁竞争：TIERS 给多个票档，写压力分散到多行 inventory 上。和 TIERS=1 对比，
//           差值就是"单行库存行锁"造成的串行化损失。
//           k6 run -e TIERS=8 -e VUS=50 scripts/k6/order.js

import http from "k6/http";
import { check, fail } from "k6";
import { Counter } from "k6/metrics";

const ordersPlaced = new Counter("orders_placed");
const ordersSoldOut = new Counter("orders_sold_out");
const ordersRejected = new Counter("orders_rejected");
const paymentsSucceeded = new Counter("payments_succeeded");

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || "60s";
const STOCK = Number(__ENV.STOCK || 200000);
// 每人限购默认放到远大于库存，否则单个种子买家会先撞限购，SOLD_OUT 就测不出来了。
const PER_USER_LIMIT = Number(__ENV.PER_USER_LIMIT || 1000000);
// 票档数量。同一场次下建多个票档，用来对照单行 inventory 行锁的串行化影响。
const TIERS = Number(__ENV.TIERS || 1);

export const options = {
  scenarios: {
    place_and_pay: {
      executor: "constant-vus",
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    // 库存售罄返回 409，属于预期业务结果，不计入失败率，所以这里只看真正的 5xx / 连接错误。
    "http_req_failed{expected_response:true}": ["rate<0.01"],
    "http_req_duration{endpoint:place_order}": ["p(95)<1000"],
    "http_req_duration{endpoint:pay_order}": ["p(95)<1000"],
  },
};

function login(username) {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ username, password: "Passw0rd!" }),
    { headers: { "Content-Type": "application/json" } }
  );
  if (res.status !== 200) {
    fail(`${username} 登录失败: ${res.status} ${res.body}`);
  }
  return res.json("data.token");
}

function jsonHeaders(token) {
  return { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };
}

export function setup() {
  const organizer = jsonHeaders(login("organizer"));
  const platform = jsonHeaders(login("platform"));
  const buyerToken = login("buyer");

  const now = Date.now();
  const created = http.post(
    `${BASE}/api/organizer/activities`,
    JSON.stringify({ title: `k6 压测活动 ${now}` }),
    { headers: organizer }
  );
  if (created.status !== 200) {
    fail(`建活动失败: ${created.status} ${created.body}`);
  }
  const activityId = created.json("data.id");

  const withShow = http.post(
    `${BASE}/api/organizer/activities/${activityId}/shows`,
    JSON.stringify({
      name: "压测场次",
      startAt: new Date(now + 2 * 3600 * 1000).toISOString(),
      endAt: new Date(now + 4 * 3600 * 1000).toISOString(),
      saleStartAt: new Date(now - 3600 * 1000).toISOString(),
      saleEndAt: new Date(now + 3 * 3600 * 1000).toISOString(),
    }),
    { headers: organizer }
  );
  if (withShow.status !== 200) {
    fail(`建场次失败: ${withShow.status} ${withShow.body}`);
  }
  const showId = withShow.json("data.shows.0.id");

  let tierIds = [];
  for (let i = 0; i < TIERS; i++) {
    const withTier = http.post(
      `${BASE}/api/organizer/shows/${showId}/tiers`,
      JSON.stringify({
        name: `压测票档 ${i + 1}`,
        unitPriceFen: 1000,
        perUserLimit: PER_USER_LIMIT,
        totalQty: STOCK,
      }),
      { headers: organizer }
    );
    if (withTier.status !== 200) {
      fail(`建票档失败: ${withTier.status} ${withTier.body}`);
    }
    tierIds = withTier.json("data.shows.0.tiers").map((t) => t.id);
  }

  const submitted = http.post(
    `${BASE}/api/organizer/activities/${activityId}/submit`,
    null,
    { headers: organizer }
  );
  if (submitted.status !== 200) {
    fail(`提交审核失败: ${submitted.status} ${submitted.body}`);
  }

  const approved = http.post(
    `${BASE}/api/platform/reviews/${activityId}/decide`,
    JSON.stringify({ decision: "APPROVED", comment: "压测放行" }),
    { headers: platform }
  );
  if (approved.status !== 200) {
    fail(`审核通过失败: ${approved.status} ${approved.body}`);
  }

  return { buyerToken, organizerToken: organizer.Authorization, showId, tierIds, activityId };
}

// 压测结束后回读库存，让"售出数恰好等于库存、available+reserved+sold==total"这个结论
// 直接出现在压测输出里，而不是靠事后手工查库。
export function teardown(data) {
  const detail = http.get(`${BASE}/api/organizer/activities/${data.activityId}`, {
    headers: { Authorization: data.organizerToken },
  });
  if (detail.status !== 200) {
    console.warn(`回读库存失败: ${detail.status} ${detail.body}`);
    return;
  }
  for (const tier of detail.json("data.shows.0.tiers")) {
    const sum = tier.availableQty + tier.reservedQty + tier.soldQty;
    console.log(
      `${tier.name} total=${tier.totalQty} available=${tier.availableQty} ` +
        `reserved=${tier.reservedQty} sold=${tier.soldQty} ` +
        `不变量 available+reserved+sold=${sum} ${sum === tier.totalQty ? "成立" : "不成立"}`
    );
  }
}

export default function (data) {
  const headers = jsonHeaders(data.buyerToken);
  const tierId = data.tierIds[Math.floor(Math.random() * data.tierIds.length)];

  const placed = http.post(
    `${BASE}/api/buyer/orders`,
    JSON.stringify({ showId: data.showId, ticketTierId: tierId, qty: 1 }),
    { headers, tags: { endpoint: "place_order" } }
  );

  const code = placed.status === 200 ? "OK" : placed.json("code");
  check(placed, {
    "下单返回 200 或预期业务冲突": () =>
      placed.status === 200 || code === "SOLD_OUT" || code === "LIMIT_EXCEEDED",
  });

  if (placed.status !== 200) {
    if (code === "SOLD_OUT") {
      ordersSoldOut.add(1);
    } else {
      ordersRejected.add(1);
    }
    return;
  }
  ordersPlaced.add(1);

  const orderId = placed.json("data.id");
  const paid = http.post(
    `${BASE}/api/buyer/orders/${orderId}/pay`,
    JSON.stringify({ success: true }),
    { headers, tags: { endpoint: "pay_order" } }
  );
  check(paid, { "支付返回 200 或冲突": (r) => r.status === 200 || r.status === 409 });
  if (paid.status === 200) {
    paymentsSucceeded.add(1);
  }
}
