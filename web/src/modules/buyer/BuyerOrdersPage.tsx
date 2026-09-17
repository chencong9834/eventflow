import { Button, Card, Table, Tag } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { http } from "../../shared/http";
import { fenToYuan, orderLabel } from "../../shared/format";
import type { ApiResponse, Order } from "../../shared/types";

export function BuyerOrdersPage() {
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: ["buyer", "orders"],
    queryFn: async () => {
      const res = (await http.get("/buyer/orders")) as ApiResponse<Order[]>;
      return res.data;
    }
  });
  return (
    <Card title="我的订单">
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data ?? []}
        scroll={{ x: 640 }}
        locale={{ emptyText: "还没有订单，去目录买一张票试试。" }}
        columns={[
          { title: "单号", dataIndex: "orderNo" },
          { title: "活动", dataIndex: "activityTitle" },
          { title: "金额", render: (_, row) => `${fenToYuan(row.amountFen)} 元` },
          {
            title: "状态",
            dataIndex: "status",
            render: (status: string) => <Tag>{orderLabel(status)}</Tag>
          },
          {
            title: "操作",
            render: (_, row) => (
              <Button type="link" onClick={() => navigate(`/buyer/orders/${row.id}`)}>
                详情
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}
