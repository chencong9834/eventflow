import { Button, Card, Table, Tabs, Tag, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { http } from "../../shared/http";
import { listPagination } from "../../shared/page";
import { reviewLabel, saleLabel } from "../../shared/format";
import type { ActivitySummary, ApiResponse, PageResult } from "../../shared/types";

const STATUSES = [
  { key: "PENDING", label: "待审" },
  { key: "APPROVED", label: "已通过" },
  { key: "REJECTED", label: "已驳回" },
  { key: "DRAFT", label: "草稿/修订中" }
];

export function ReviewListPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState("PENDING");
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const query = useQuery({
    queryKey: ["platform", "reviews", status, page, size],
    queryFn: async () => {
      const res = (await http.get("/platform/reviews", { params: { status, page, size } })) as ApiResponse<
        PageResult<ActivitySummary>
      >;
      return res.data;
    }
  });

  return (
    <Card>
      <Typography.Title level={3}>活动审核</Typography.Title>
      <Typography.Paragraph type="secondary">
        待审活动可审批。通过后主办方改价或改结构需先下架修订再重新提交。
      </Typography.Paragraph>
      <Tabs
        activeKey={status}
        onChange={(key) => {
          setStatus(key);
          setPage(1);
        }}
        items={STATUSES.map((item) => ({ key: item.key, label: item.label }))}
      />
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.items ?? []}
        pagination={listPagination(page, size, query.data?.total ?? 0, (nextPage, nextSize) => {
          setPage(nextPage);
          setSize(nextSize);
        })}
        columns={[
          { title: "活动", dataIndex: "title" },
          { title: "主办方", dataIndex: "organizerName" },
          { title: "编码", dataIndex: "organizerCode" },
          {
            title: "状态",
            dataIndex: "reviewStatus",
            render: (value: string) => <Tag>{reviewLabel(value)}</Tag>
          },
          {
            title: "售卖",
            dataIndex: "saleStatus",
            render: (value: string) => <Tag>{saleLabel(value)}</Tag>
          },
          {
            title: "操作",
            render: (_, row) => (
              <Button type="link" onClick={() => navigate(`/platform/reviews/${row.id}`)}>
                {row.reviewStatus === "PENDING" ? "审批" : "查看"}
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}
