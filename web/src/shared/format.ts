export function toDatetimeLocal(iso?: string) {
  if (!iso) {
    return "";
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function fromDatetimeLocal(value: string) {
  return new Date(value).toISOString();
}

export function reviewLabel(status: string) {
  if (status === "DRAFT") return "草稿";
  if (status === "PENDING") return "待审";
  if (status === "APPROVED") return "已通过";
  if (status === "REJECTED") return "已驳回";
  return status;
}

export function saleLabel(status: string) {
  return status === "ON_SALE" ? "在售" : "停售";
}

export function fenToYuan(fen: number) {
  return (fen / 100).toFixed(2);
}

export function orderLabel(status: string) {
  const map: Record<string, string> = {
    CREATED: "待支付",
    PAID: "已支付",
    CANCELLED: "已关闭",
    REFUNDED: "已退款",
    FULFILLED: "已完成"
  };
  return map[status] ?? status;
}

export function ticketLabel(status: string) {
  if (status === "UNUSED") return "未使用";
  if (status === "USED") return "已核销";
  if (status === "VOID") return "已作废";
  return status;
}
