import { Button, Card, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { reviewLabel, saleLabel } from "../../shared/format";
import type { ActivitySummary, ApiResponse } from "../../shared/types";
import { useAuth } from "../../auth/AuthProvider";

export function ActivityListPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const canWrite = (user?.permissions ?? []).includes("activity:write");
  const query = useQuery({
    queryKey: ["organizer", "activities"],
    queryFn: async () => {
      const res = (await http.get("/organizer/activities")) as ApiResponse<ActivitySummary[]>;
      return res.data;
    }
  });
  const create = useMutation({
    mutationFn: async () => {
      const res = (await http.post("/organizer/activities", { title: "未命名活动" })) as ApiResponse<ActivitySummary>;
      return res.data;
    },
    onSuccess: async (activity) => {
      await queryClient.invalidateQueries({ queryKey: ["organizer", "activities"] });
      navigate(`/organizer/activities/${activity.id}`);
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "创建失败")
  });

  return (
    <Card>
      <Space style={{ width: "100%", justifyContent: "space-between", marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          活动
        </Typography.Title>
        {canWrite ? (
          <Button type="primary" loading={create.isPending} onClick={() => create.mutate()}>
            新建活动
          </Button>
        ) : null}
      </Space>
      <Typography.Paragraph type="secondary">
        草稿可维护场次与票档；提交后由平台审核。已通过的活动改价或改结构前需先申请修订并重新送审。
      </Typography.Paragraph>
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data ?? []}
        columns={[
          { title: "标题", dataIndex: "title" },
          {
            title: "审核",
            dataIndex: "reviewStatus",
            width: 110,
            render: (status: string) => <Tag>{reviewLabel(status)}</Tag>
          },
          {
            title: "售卖",
            dataIndex: "saleStatus",
            width: 90,
            render: (status: string) => (
              <Tag color={status === "ON_SALE" ? "green" : "default"}>{saleLabel(status)}</Tag>
            )
          },
          { title: "场次数", dataIndex: "showCount", width: 90 },
          {
            title: "操作",
            width: 100,
            render: (_, row) => (
              <Button type="link" onClick={() => navigate(`/organizer/activities/${row.id}`)}>
                详情
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}
