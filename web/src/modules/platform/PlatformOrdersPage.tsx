import { Button, Card, Input, Space, Table, Tag, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { http } from "../../shared/http";
import { fenToYuan, fromDatetimeLocal, orderLabel, toDatetimeLocal } from "../../shared/format";
import type { ApiResponse, Order } from "../../shared/types";

function defaultRange() {
  const to = new Date();
  to.setDate(to.getDate() + 1);
  const from = new Date();
  from.setDate(from.getDate() - 7);
  return { from: toDatetimeLocal(from.toISOString()), to: toDatetimeLocal(to.toISOString()) };
}

export function PlatformOrdersPage() {
  const navigate = useNavigate();
  const initial = useMemo(() => defaultRange(), []);
  const [fromLocal, setFromLocal] = useState(initial.from);
  const [toLocal, setToLocal] = useState(initial.to);
  const from = fromDatetimeLocal(fromLocal);
  const to = fromDatetimeLocal(toLocal);
  const query = useQuery({
    queryKey: ["platform", "orders", from, to],
    enabled: Boolean(from && to),
    queryFn: async () => {
      const res = (await http.get("/platform/orders", { params: { from, to } })) as ApiResponse<Order[]>;
      return res.data;
    }
  });

  return (
    <Card>
      <Space style={{ width: "100%", justifyContent: "space-between", marginBottom: 16 }} wrap>
        <Typography.Title level={3} style={{ margin: 0 }}>
          跨租户订单
        </Typography.Title>
        <Space>
          <Input type="datetime-local" value={fromLocal} onChange={(e) => setFromLocal(e.target.value)} />
          <span>至</span>
          <Input type="datetime-local" value={toLocal} onChange={(e) => setToLocal(e.target.value)} />
        </Space>
      </Space>
      <Typography.Paragraph type="secondary">
        按创建时间查询，最多 200 条。客服可对已支付且未核销的订单做整单退款。
      </Typography.Paragraph>
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data ?? []}
        columns={[
          { title: "单号", dataIndex: "orderNo" },
          { title: "活动", dataIndex: "activityTitle" },
          { title: "数量", dataIndex: "qty", width: 80 },
          { title: "金额", render: (_, row) => `${fenToYuan(row.amountFen)} 元` },
          { title: "状态", dataIndex: "status", render: (s: string) => <Tag>{orderLabel(s)}</Tag> },
          {
            title: "操作",
            width: 100,
            render: (_, row) => (
              <Button type="link" onClick={() => navigate(`/platform/orders/${row.id}`)}>
                详情
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}
