import { Button, Card, Table, Tag } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { http } from "../../shared/http";
import { listPagination } from "../../shared/page";
import { fenToYuan, orderLabel } from "../../shared/format";
import type { ApiResponse, Order, PageResult } from "../../shared/types";

export function OrganizerOrdersPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const query = useQuery({
    queryKey: ["organizer", "orders", page, size],
    queryFn: async () => {
      const res = (await http.get("/organizer/orders", { params: { page, size } })) as ApiResponse<PageResult<Order>>;
      return res.data;
    }
  });
  return (
    <Card title="本租户订单">
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.items ?? []}
        pagination={listPagination(page, size, query.data?.total ?? 0, (nextPage, nextSize) => {
          setPage(nextPage);
          setSize(nextSize);
        })}
        columns={[
          { title: "单号", dataIndex: "orderNo" },
          { title: "活动", dataIndex: "activityTitle" },
          { title: "数量", dataIndex: "qty" },
          { title: "金额", render: (_, row) => `${fenToYuan(row.amountFen)} 元` },
          { title: "状态", dataIndex: "status", render: (s: string) => <Tag>{orderLabel(s)}</Tag> },
          {
            title: "操作",
            render: (_, row) => (
              <Button type="link" onClick={() => navigate(`/organizer/orders/${row.id}`)}>
                详情
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}
