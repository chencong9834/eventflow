import { Card, List } from "antd";
import { useQuery } from "@tanstack/react-query";
import { http } from "../../shared/http";
import type { ApiResponse, SiteNotice } from "../../shared/types";

export function NoticesPage() {
  const query = useQuery({
    queryKey: ["notices"],
    queryFn: async () => {
      const res = (await http.get("/notices")) as ApiResponse<SiteNotice[]>;
      return res.data;
    }
  });
  return (
    <Card title="站内通知">
      <List
        loading={query.isLoading}
        dataSource={query.data ?? []}
        locale={{ emptyText: "暂无通知" }}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta title={item.title} description={`${item.body} · ${item.createdAt ?? ""}`} />
          </List.Item>
        )}
      />
    </Card>
  );
}
