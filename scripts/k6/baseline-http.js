// 压测链路本身的基准：只打不碰数据库的健康检查端点。
// 用途是把"压测机 + 网络"的吞吐上限和"应用"的吞吐上限区分开——
// 如果这个脚本的 req/s 和业务脚本接近，说明测到的是链路上限，不是应用能力。
//
//   k6 run -e BASE_URL=http://host.docker.internal:8080 -e VUS=50 scripts/k6/baseline-http.js

import http from "k6/http";
import { check } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const VUS = Number(__ENV.VUS || 50);
const DURATION = __ENV.DURATION || "30s";

export const options = {
  scenarios: {
    health: {
      executor: "constant-vus",
      vus: VUS,
      duration: DURATION,
    },
  },
};

export default function () {
  const res = http.get(`${BASE}/actuator/health`);
  check(res, { "health 200": (r) => r.status === 200 });
}
