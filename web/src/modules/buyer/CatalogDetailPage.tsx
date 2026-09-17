import { Button, Card, InputNumber, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { fenToYuan, toDatetimeLocal } from "../../shared/format";
import type { ActivityDetail, ApiResponse, Order } from "../../shared/types";
import { CoverImage } from "../../shared/CoverImage";

export function CatalogDetailPage() {
  const { activityId } = useParams();
  const navigate = useNavigate();
  const [qtyByTier, setQtyByTier] = useState<Record<string, number>>({});
  const query = useQuery({
    queryKey: ["catalog", activityId],
    enabled: Boolean(activityId),
    queryFn: async () => {
      const res = (await http.get(`/catalog/activities/${activityId}`)) as ApiResponse<ActivityDetail>;
      return res.data;
    }
  });
  const order = useMutation({
    mutationFn: async (payload: { showId: string; ticketTierId: string; qty: number }) => {
      const res = (await http.post("/buyer/orders", payload)) as ApiResponse<Order>;
      return res.data;
    },
    onSuccess: (created) => {
      message.success("下单成功，请模拟支付");
      navigate(`/buyer/orders/${created.id}`);
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "下单失败")
  });
  const activity = query.data;
  const now = Date.now();

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Button onClick={() => navigate("/buyer/home")}>返回目录</Button>
      <Card loading={query.isLoading} styles={{ body: { padding: 0 } }}>
        <CoverImage src={activity?.coverUrl} title={activity?.title ?? "活动"} height={220} />
        <div style={{ padding: 20 }}>
          <Typography.Title level={3} style={{ marginTop: 0 }}>
            {activity?.title}
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
            主办方 {activity?.organizerName || "—"}
          </Typography.Paragraph>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            {activity?.description || "暂无简介"}
          </Typography.Paragraph>
        </div>
      </Card>
      {(activity?.shows ?? []).map((show) => {
        const open = new Date(show.saleStartAt).getTime() <= now && now <= new Date(show.saleEndAt).getTime();
        return (
          <Card
            key={show.id}
            title={show.name}
            extra={open ? <Tag color="green">售卖中</Tag> : <Tag>未开售/已停售</Tag>}
          >
            <Typography.Paragraph type="secondary">
              {toDatetimeLocal(show.startAt).replace("T", " ")} ~ {toDatetimeLocal(show.endAt).replace("T", " ")}
            </Typography.Paragraph>
            <Table
              rowKey="id"
              pagination={false}
              scroll={{ x: 560 }}
              dataSource={show.tiers}
              columns={[
                { title: "票档", dataIndex: "name" },
                { title: "单价", render: (_, row) => `${fenToYuan(row.unitPriceFen)} 元` },
                { title: "限购", dataIndex: "perUserLimit", width: 80 },
                { title: "可售", dataIndex: "availableQty", width: 80 },
                {
                  title: "购买",
                  width: 220,
                  render: (_, row) => {
                    const qty = qtyByTier[row.id] ?? 1;
                    return (
                      <Space wrap>
                        <InputNumber
                          min={1}
                          max={Math.max(1, Math.min(row.perUserLimit, row.availableQty || 1))}
                          value={qty}
                          onChange={(v) => setQtyByTier((prev) => ({ ...prev, [row.id]: v ?? 1 }))}
                        />
                        <Button
                          type="primary"
                          disabled={!open || row.availableQty < 1}
                          loading={order.isPending}
                          onClick={() => order.mutate({ showId: show.id, ticketTierId: row.id, qty })}
                        >
                          下单
                        </Button>
                      </Space>
                    );
                  }
                }
              ]}
            />
          </Card>
        );
      })}
    </Space>
  );
}
