import { Card, DatePicker, Space, Statistic } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { http } from "../../shared/http";
import { fenToYuan } from "../../shared/format";
import type { ApiResponse, ReportSummary } from "../../shared/types";

export function ReportPage() {
  const now = new Date();
  const fromDefault = new Date(now.getTime() - 30 * 86400000).toISOString();
  const toDefault = new Date(now.getTime() + 86400000).toISOString();
  const [from, setFrom] = useState(fromDefault);
  const [to, setTo] = useState(toDefault);
  const query = useQuery({
    queryKey: ["organizer", "reports", from, to],
    queryFn: async () => {
      const res = (await http.get("/organizer/reports", { params: { from, to } })) as ApiResponse<ReportSummary>;
      return res.data;
    }
  });
  const data = query.data;

  return (
    <Card title="经营报表">
      <Space style={{ marginBottom: 24 }}>
        <DatePicker
          showTime
          placeholder="开始"
          onChange={(_, value) => {
            if (typeof value === "string" && value) {
              setFrom(new Date(value).toISOString());
            }
          }}
        />
        <DatePicker
          showTime
          placeholder="结束"
          onChange={(_, value) => {
            if (typeof value === "string" && value) {
              setTo(new Date(value).toISOString());
            }
          }}
        />
      </Space>
      <Space size={48}>
        <Statistic title="已支付订单" value={data?.paidOrderCount ?? 0} loading={query.isLoading} />
        <Statistic title="售票张数" value={data?.soldQty ?? 0} loading={query.isLoading} />
        <Statistic title="销售额（元）" value={data ? fenToYuan(data.salesFen) : "0.00"} loading={query.isLoading} />
        <Statistic title="退款额（元）" value={data ? fenToYuan(data.refundFen) : "0.00"} loading={query.isLoading} />
        <Statistic title="已核销" value={data?.usedTicketCount ?? 0} loading={query.isLoading} />
      </Space>
    </Card>
  );
}
