import { Card, List, Pagination } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { http } from "../../shared/http";
import type { ApiResponse, PageResult, SiteNotice } from "../../shared/types";

export function NoticesPage() {
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const query = useQuery({
    queryKey: ["notices", page, size],
    queryFn: async () => {
      const res = (await http.get("/notices", { params: { page, size } })) as ApiResponse<PageResult<SiteNotice>>;
      return res.data;
    }
  });
  return (
    <Card title="站内通知">
      <List
        loading={query.isLoading}
        dataSource={query.data?.items ?? []}
        locale={{ emptyText: "暂无通知" }}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta title={item.title} description={`${item.body} · ${item.createdAt ?? ""}`} />
          </List.Item>
        )}
      />
      {(query.data?.total ?? 0) > 0 ? (
        <Pagination
          style={{ marginTop: 16, textAlign: "right" }}
          current={page}
          pageSize={size}
          total={query.data?.total ?? 0}
          showSizeChanger
          pageSizeOptions={["10", "20", "50", "100"]}
          onChange={(nextPage, nextSize) => {
            setPage(nextSize === size ? nextPage : 1);
            setSize(nextSize);
          }}
        />
      ) : null}
    </Card>
  );
}
