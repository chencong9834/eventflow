// 目录读路径压测：JWT 鉴权 + 跨租户只读目录查询的吞吐和延迟。
//
//   k6 run scripts/k6/catalog.js
//   k6 run -e BASE_URL=http://host.docker.internal:8080 -e VUS=50 scripts/k6/catalog.js
//
// 登录本身按 IP 限流（eventflow.auth.login-max-attempts-per-ip），是有意的防爆破设计，
// 不是吞吐指标，所以这里只在 setup 里登录一次，之后复用 JWT——和真实客户端行为一致。

import http from "k6/http";
import { check } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || "60s";

export const options = {
  scenarios: {
    catalog_read: {
      executor: "constant-vus",
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{endpoint:catalog_list}": ["p(95)<500"],
    "http_req_duration{endpoint:catalog_detail}": ["p(95)<500"],
  },
};

export function setup() {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ username: "buyer", password: "Passw0rd!" }),
    { headers: { "Content-Type": "application/json" } }
  );
  if (res.status !== 200) {
    throw new Error(`buyer 登录失败: ${res.status} ${res.body}`);
  }
  return { token: res.json("data.token") };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  const list = http.get(`${BASE}/api/catalog/activities`, {
    headers,
    tags: { endpoint: "catalog_list" },
  });
  check(list, { "目录列表 200": (r) => r.status === 200 });
  if (list.status !== 200) {
    return;
  }

  const activities = list.json("data") || [];
  if (activities.length === 0) {
    return;
  }

  const detail = http.get(`${BASE}/api/catalog/activities/${activities[0].id}`, {
    headers,
    tags: { endpoint: "catalog_detail" },
  });
  check(detail, { "目录详情 200": (r) => r.status === 200 });
}
