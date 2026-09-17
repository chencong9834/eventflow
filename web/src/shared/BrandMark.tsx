export function BrandMark({ light = false }: { light?: boolean }) {
  return (
    <span className={light ? "brand-mark brand-mark-light" : "brand-mark"}>
      <span className="brand-mark-badge">E</span>
      EventFlow
    </span>
  );
}

export const DEMO_ACCOUNTS = [
  { username: "buyer", password: "Passw0rd!", role: "购票用户", hint: "目录购票、模拟支付、看票" },
  { username: "organizer", password: "Passw0rd!", role: "主办方", hint: "活动、订单、核销、报表" },
  { username: "platform", password: "Passw0rd!", role: "平台运营", hint: "租户、审核、客服退款" }
];
