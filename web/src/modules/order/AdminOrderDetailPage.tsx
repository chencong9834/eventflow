import { Button, Card, Descriptions, Space, Table, Tag, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { fenToYuan, orderLabel, ticketLabel } from "../../shared/format";
import type { ApiResponse, Order } from "../../shared/types";
import { useAuth } from "../../auth/AuthProvider";

function AdminOrderDetail({
  scope,
  refundLabel
}: {
  scope: "organizer" | "platform";
  refundLabel: string;
}) {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const canRefund = (user?.permissions ?? []).includes("refund:write");
  const base = `/${scope}/orders`;
  const query = useQuery({
    queryKey: [scope, "orders", orderId],
    enabled: Boolean(orderId),
    queryFn: async () => {
      const res = (await http.get(`${base}/${orderId}`)) as ApiResponse<Order>;
      return res.data;
    }
  });
  const refund = useMutation({
    mutationFn: async () => {
      await http.post(`${base}/${orderId}/refund`);
    },
    onSuccess: async () => {
      message.success(refundLabel + "成功");
      await queryClient.invalidateQueries({ queryKey: [scope, "orders", orderId] });
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "退款失败")
  });
  const order = query.data;

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Button onClick={() => navigate(base)}>返回列表</Button>
      <Card loading={query.isLoading} title={order?.orderNo}>
        <Descriptions bordered column={1}>
          <Descriptions.Item label="活动">{order?.activityTitle}</Descriptions.Item>
          <Descriptions.Item label="场次 / 票档">
            {order?.showName} / {order?.tierName}
          </Descriptions.Item>
          <Descriptions.Item label="金额">{order ? `${fenToYuan(order.amountFen)} 元` : ""}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag>{orderLabel(order?.status ?? "")}</Tag>
          </Descriptions.Item>
        </Descriptions>
        {canRefund && order?.status === "PAID" ? (
          <Button danger style={{ marginTop: 16 }} loading={refund.isPending} onClick={() => refund.mutate()}>
            {refundLabel}
          </Button>
        ) : null}
      </Card>
      <Card title="票券">
        <Table
          rowKey="id"
          pagination={false}
          dataSource={order?.tickets ?? []}
          columns={[
            { title: "票号", dataIndex: "ticketNo" },
            { title: "核销码", dataIndex: "verifyCode" },
            { title: "状态", dataIndex: "status", render: (s: string) => ticketLabel(s) }
          ]}
        />
      </Card>
    </Space>
  );
}

export function OrganizerOrderDetailPage() {
  return <AdminOrderDetail scope="organizer" refundLabel="整单退款" />;
}

export function PlatformOrderDetailPage() {
  return <AdminOrderDetail scope="platform" refundLabel="客服整单退款" />;
}
