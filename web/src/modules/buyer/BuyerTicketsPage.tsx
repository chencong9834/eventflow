import { Card, Table } from "antd";
import { useQuery } from "@tanstack/react-query";
import { http } from "../../shared/http";
import { ticketLabel } from "../../shared/format";
import type { ApiResponse, IssuedTicket } from "../../shared/types";

export function BuyerTicketsPage() {
  const query = useQuery({
    queryKey: ["buyer", "tickets"],
    queryFn: async () => {
      const res = (await http.get("/buyer/tickets")) as ApiResponse<IssuedTicket[]>;
      return res.data;
    }
  });
  return (
    <Card title="我的票">
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data ?? []}
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
