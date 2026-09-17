import { Button, Card, Descriptions, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { fenToYuan, orderLabel, ticketLabel } from "../../shared/format";
import type { ApiResponse, Order } from "../../shared/types";

export function BuyerOrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["buyer", "orders", orderId],
    enabled: Boolean(orderId),
    queryFn: async () => {
      const res = (await http.get(`/buyer/orders/${orderId}`)) as ApiResponse<Order>;
      return res.data;
    }
  });
  const pay = useMutation({
    mutationFn: async (success: boolean) => {
      const res = (await http.post(`/buyer/orders/${orderId}/pay`, { success })) as ApiResponse<Order>;
      return res.data;
    },
    onSuccess: async () => {
      message.success("已处理模拟支付");
      await queryClient.invalidateQueries({ queryKey: ["buyer", "orders", orderId] });
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "支付失败")
  });
  const cancel = useMutation({
    mutationFn: async () => {
      await http.post(`/buyer/orders/${orderId}/cancel`);
    },
    onSuccess: async () => {
      message.success("已取消");
      await queryClient.invalidateQueries({ queryKey: ["buyer", "orders", orderId] });
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "取消失败")
  });
  const order = query.data;

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Button onClick={() => navigate("/buyer/orders")}>返回订单</Button>
      <Card loading={query.isLoading} title={order?.orderNo}>
        <Descriptions bordered column={1}>
          <Descriptions.Item label="活动">{order?.activityTitle}</Descriptions.Item>
          <Descriptions.Item label="场次">{order?.showName}</Descriptions.Item>
          <Descriptions.Item label="票档">{order?.tierName}</Descriptions.Item>
          <Descriptions.Item label="数量">{order?.qty}</Descriptions.Item>
          <Descriptions.Item label="金额">{order ? `${fenToYuan(order.amountFen)} 元` : ""}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag>{orderLabel(order?.status ?? "")}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="支付截止">{order?.payDeadlineAt}</Descriptions.Item>
        </Descriptions>
        {order?.status === "CREATED" ? (
          <Space wrap style={{ marginTop: 16 }}>
            <Button type="primary" loading={pay.isPending} onClick={() => pay.mutate(true)}>
              模拟支付成功
            </Button>
            <Button loading={pay.isPending} onClick={() => pay.mutate(false)}>
              模拟支付失败
            </Button>
            <Button danger loading={cancel.isPending} onClick={() => cancel.mutate()}>
              取消订单
            </Button>
          </Space>
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
        {order?.tickets?.length ? (
          <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
            入场出示核销码。
          </Typography.Paragraph>
        ) : null}
      </Card>
    </Space>
  );
}
