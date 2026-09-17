import { Button, Card, Descriptions, Form, Input, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate, useParams } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { fenToYuan, reviewLabel, saleLabel, toDatetimeLocal } from "../../shared/format";
import type { ActivityDetail, ApiResponse } from "../../shared/types";

const schema = z.object({
  comment: z.string().max(512).optional()
});

export function ReviewDetailPage() {
  const { activityId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const form = useForm<{ comment?: string }>({ resolver: zodResolver(schema), defaultValues: { comment: "" } });
  const query = useQuery({
    queryKey: ["platform", "reviews", activityId],
    enabled: Boolean(activityId),
    queryFn: async () => {
      const res = (await http.get(`/platform/reviews/${activityId}`)) as ApiResponse<ActivityDetail>;
      return res.data;
    }
  });
  const decide = useMutation({
    mutationFn: async (decision: "APPROVED" | "REJECTED") => {
      const comment = form.getValues("comment");
      await http.post(`/platform/reviews/${activityId}/decide`, { decision, comment });
    },
    onSuccess: async (_, decision) => {
      message.success(decision === "APPROVED" ? "已通过" : "已驳回");
      await queryClient.invalidateQueries({ queryKey: ["platform", "reviews"] });
      navigate("/platform/reviews");
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "操作失败")
  });
  const activity = query.data;

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Button onClick={() => navigate("/platform/reviews")}>返回列表</Button>
      <Card loading={query.isLoading} title={activity?.title}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="主办方">{activity?.organizerName}</Descriptions.Item>
          <Descriptions.Item label="编码">{activity?.organizerCode}</Descriptions.Item>
          <Descriptions.Item label="审核">
            <Tag>{reviewLabel(activity?.reviewStatus ?? "")}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="售卖">
            <Tag>{saleLabel(activity?.saleStatus ?? "")}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="简介" span={2}>
            {activity?.description || "—"}
          </Descriptions.Item>
          <Descriptions.Item label="封面" span={2}>
            {activity?.coverUrl ? <img src={activity.coverUrl} alt="封面" style={{ maxWidth: 240 }} /> : "—"}
          </Descriptions.Item>
        </Descriptions>
      </Card>
      {(activity?.shows ?? []).map((show) => (
        <Card key={show.id} title={show.name}>
          <Typography.Paragraph type="secondary">
            {toDatetimeLocal(show.startAt).replace("T", " ")} ~ {toDatetimeLocal(show.endAt).replace("T", " ")}
          </Typography.Paragraph>
          <Table
            rowKey="id"
            pagination={false}
            dataSource={show.tiers}
            columns={[
              { title: "票档", dataIndex: "name" },
              { title: "单价（元）", render: (_, row) => fenToYuan(row.unitPriceFen) },
              { title: "限购", dataIndex: "perUserLimit" },
              { title: "库存", dataIndex: "totalQty" }
            ]}
          />
        </Card>
      ))}
      <Card title="审批">
        <Form layout="vertical">
          <Form.Item label="意见（驳回必填）">
            <Controller name="comment" control={form.control} render={({ field }) => <Input.TextArea {...field} rows={3} />} />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              loading={decide.isPending}
              disabled={activity?.reviewStatus !== "PENDING"}
              onClick={() => decide.mutate("APPROVED")}
            >
              通过
            </Button>
            <Button
              danger
              loading={decide.isPending}
              disabled={activity?.reviewStatus !== "PENDING"}
              onClick={() => decide.mutate("REJECTED")}
            >
              驳回
            </Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
