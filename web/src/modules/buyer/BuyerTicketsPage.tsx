import { Card, Table } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { http } from "../../shared/http";
import { listPagination } from "../../shared/page";
import { ticketLabel } from "../../shared/format";
import type { ApiResponse, IssuedTicket, PageResult } from "../../shared/types";

export function BuyerTicketsPage() {
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const query = useQuery({
    queryKey: ["buyer", "tickets", page, size],
    queryFn: async () => {
      const res = (await http.get("/buyer/tickets", { params: { page, size } })) as ApiResponse<
        PageResult<IssuedTicket>
      >;
      return res.data;
    }
  });
  return (
    <Card title="我的票">
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.items ?? []}
        pagination={listPagination(page, size, query.data?.total ?? 0, (nextPage, nextSize) => {
          setPage(nextPage);
          setSize(nextSize);
        })}
        scroll={{ x: 480 }}
        locale={{ emptyText: "支付成功后票券会出现在这里。" }}
        columns={[
          { title: "票号", dataIndex: "ticketNo" },
          { title: "核销码", dataIndex: "verifyCode" },
          { title: "状态", dataIndex: "status", render: (s: string) => ticketLabel(s) }
        ]}
      />
    </Card>
  );
}
