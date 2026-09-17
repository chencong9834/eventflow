import { Button, Card, Descriptions, Form, Input, InputNumber, Modal, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate, useParams } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { fenToYuan, fromDatetimeLocal, reviewLabel, saleLabel, toDatetimeLocal } from "../../shared/format";
import type { ActivityDetail, ActivityShow, ApiResponse, TicketTier } from "../../shared/types";
import { useAuth } from "../../auth/AuthProvider";

const activitySchema = z.object({
  title: z.string().trim().min(1, "请输入标题").max(128),
  description: z.string().max(4000).optional(),
  coverUrl: z.string().max(512).optional()
});

const showSchema = z.object({
  name: z.string().trim().min(1).max(128),
  startAt: z.string().min(1, "请选择开演时间"),
  endAt: z.string().min(1, "请选择结束时间"),
  saleStartAt: z.string().min(1),
  saleEndAt: z.string().min(1)
});

const tierSchema = z.object({
  name: z.string().trim().min(1).max(64),
  unitPriceFen: z.coerce.number().int().min(0),
  perUserLimit: z.coerce.number().int().min(1),
  totalQty: z.coerce.number().int().min(1)
});

type ActivityForm = z.infer<typeof activitySchema>;
type ShowForm = z.infer<typeof showSchema>;
type TierForm = z.infer<typeof tierSchema>;

function showToForm(show: ActivityShow): ShowForm {
  return {
    name: show.name,
    startAt: toDatetimeLocal(show.startAt),
    endAt: toDatetimeLocal(show.endAt),
    saleStartAt: toDatetimeLocal(show.saleStartAt),
    saleEndAt: toDatetimeLocal(show.saleEndAt)
  };
}

export function ActivityDetailPage() {
  const { activityId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const canWrite = (user?.permissions ?? []).includes("activity:write");
  const [showOpen, setShowOpen] = useState(false);
  const [editingShow, setEditingShow] = useState<ActivityShow | null>(null);
  const [tierShowId, setTierShowId] = useState<string | null>(null);
  const [editingTier, setEditingTier] = useState<{ showId: string; tier: TicketTier } | null>(null);

  const query = useQuery({
    queryKey: ["organizer", "activities", activityId],
    enabled: Boolean(activityId),
    queryFn: async () => {
      const res = (await http.get(`/organizer/activities/${activityId}`)) as ApiResponse<ActivityDetail>;
      return res.data;
    }
  });
  const activity = query.data;
  const editable = activity?.reviewStatus === "DRAFT" || activity?.reviewStatus === "REJECTED";

  const activityForm = useForm<ActivityForm>({
    resolver: zodResolver(activitySchema),
    values: {
      title: activity?.title ?? "",
      description: activity?.description ?? "",
      coverUrl: activity?.coverUrl ?? ""
    }
  });
  const showForm = useForm<ShowForm>({
    resolver: zodResolver(showSchema),
    defaultValues: { name: "", startAt: "", endAt: "", saleStartAt: "", saleEndAt: "" }
  });
  const tierForm = useForm<TierForm>({
    resolver: zodResolver(tierSchema),
    defaultValues: { name: "普通票", unitPriceFen: 9900, perUserLimit: 4, totalQty: 100 }
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["organizer", "activities", activityId] });
  };

  const saveActivity = useMutation({
    mutationFn: async (values: ActivityForm) => {
      await http.put(`/organizer/activities/${activityId}`, values);
    },
    onSuccess: async () => {
      message.success("已保存");
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "保存失败")
  });
  const addShow = useMutation({
    mutationFn: async (values: ShowForm) => {
      await http.post(`/organizer/activities/${activityId}/shows`, {
        name: values.name,
        startAt: fromDatetimeLocal(values.startAt),
        endAt: fromDatetimeLocal(values.endAt),
        saleStartAt: fromDatetimeLocal(values.saleStartAt),
        saleEndAt: fromDatetimeLocal(values.saleEndAt)
      });
    },
    onSuccess: async () => {
      message.success("已添加场次");
      setShowOpen(false);
      showForm.reset();
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "添加失败")
  });
  const updateShow = useMutation({
    mutationFn: async (payload: ShowForm & { id: string }) => {
      await http.put(`/organizer/shows/${payload.id}`, {
        name: payload.name,
        startAt: fromDatetimeLocal(payload.startAt),
        endAt: fromDatetimeLocal(payload.endAt),
        saleStartAt: fromDatetimeLocal(payload.saleStartAt),
        saleEndAt: fromDatetimeLocal(payload.saleEndAt)
      });
    },
    onSuccess: async () => {
      message.success("已更新场次");
      setEditingShow(null);
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "更新失败")
  });
  const addTier = useMutation({
    mutationFn: async (values: TierForm & { showId: string }) => {
      await http.post(`/organizer/shows/${values.showId}/tiers`, values);
    },
    onSuccess: async () => {
      message.success("已添加票档");
      setTierShowId(null);
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "添加失败")
  });
  const updateTier = useMutation({
    mutationFn: async (values: TierForm & { id: string }) => {
      await http.put(`/organizer/tiers/${values.id}`, values);
    },
    onSuccess: async () => {
      message.success("已更新票档");
      setEditingTier(null);
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "更新失败")
  });
  const submit = useMutation({
    mutationFn: async () => {
      await http.post(`/organizer/activities/${activityId}/submit`);
    },
    onSuccess: async () => {
      message.success("已提交审核");
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "提交失败")
  });
  const revision = useMutation({
    mutationFn: async () => {
      await http.post(`/organizer/activities/${activityId}/revision`);
    },
    onSuccess: async () => {
      message.success("已下架并打开修订，改完后请重新提交审核");
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "操作失败")
  });
  const onSale = useMutation({
    mutationFn: async () => {
      await http.post(`/organizer/activities/${activityId}/on-sale`);
    },
    onSuccess: async () => {
      message.success("已重新开售");
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "开售失败")
  });
  const offSale = useMutation({
    mutationFn: async () => {
      await http.post(`/organizer/activities/${activityId}/off-sale`);
    },
    onSuccess: async () => {
      message.success("已下架停售");
      await invalidate();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "下架失败")
  });

  if (query.isError) {
    return (
      <Card>
        <Typography.Text type="danger">加载失败</Typography.Text>
      </Card>
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Button onClick={() => navigate("/organizer/activities")}>返回列表</Button>
      <Card loading={query.isLoading} title={activity?.title}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Tag>{reviewLabel(activity?.reviewStatus ?? "")}</Tag>
          <Tag color={activity?.saleStatus === "ON_SALE" ? "green" : "default"}>
            {saleLabel(activity?.saleStatus ?? "")}
          </Tag>
          {canWrite && editable ? (
            <Button type="primary" loading={submit.isPending} onClick={() => submit.mutate()}>
              提交审核
            </Button>
          ) : null}
          {canWrite && activity?.reviewStatus === "APPROVED" ? (
            <Button onClick={() => revision.mutate()} loading={revision.isPending}>
              改价/改结构（下架修订）
            </Button>
          ) : null}
          {canWrite && activity?.reviewStatus === "APPROVED" && activity.saleStatus === "CLOSED" ? (
            <Button type="primary" loading={onSale.isPending} onClick={() => onSale.mutate()}>
              重新开售
            </Button>
          ) : null}
          {canWrite && activity?.reviewStatus === "APPROVED" && activity.saleStatus === "ON_SALE" ? (
            <Button danger loading={offSale.isPending} onClick={() => offSale.mutate()}>
              下架停售
            </Button>
          ) : null}
        </Space>
        {canWrite && editable ? (
          <Form layout="vertical">
            <Form.Item
              label="标题"
              validateStatus={activityForm.formState.errors.title ? "error" : ""}
              help={activityForm.formState.errors.title?.message}
            >
              <Controller name="title" control={activityForm.control} render={({ field }) => <Input {...field} />} />
            </Form.Item>
            <Form.Item label="简介">
              <Controller
                name="description"
                control={activityForm.control}
                render={({ field }) => <Input.TextArea {...field} rows={3} />}
              />
            </Form.Item>
            <Form.Item label="封面 URL">
              <Controller name="coverUrl" control={activityForm.control} render={({ field }) => <Input {...field} />} />
            </Form.Item>
            <Button loading={saveActivity.isPending} onClick={activityForm.handleSubmit((v) => saveActivity.mutate(v))}>
              保存基本信息
            </Button>
          </Form>
        ) : (
          <Descriptions bordered column={1}>
            <Descriptions.Item label="简介">{activity?.description || "—"}</Descriptions.Item>
            <Descriptions.Item label="封面">
              {activity?.coverUrl ? <img src={activity.coverUrl} alt="封面" style={{ maxWidth: 240 }} /> : "—"}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>
      <Card
        title="场次与票档"
        extra={
          canWrite && editable ? (
            <Button
              type="primary"
              onClick={() => {
                showForm.reset({ name: "", startAt: "", endAt: "", saleStartAt: "", saleEndAt: "" });
                setShowOpen(true);
              }}
            >
              添加场次
            </Button>
          ) : null
        }
      >
        {(activity?.shows ?? []).map((show) => (
          <Card
            key={show.id}
            type="inner"
            style={{ marginBottom: 12 }}
            title={show.name}
            extra={
              canWrite && editable ? (
                <Space>
                  <Button
                    size="small"
                    onClick={() => {
                      showForm.reset(showToForm(show));
                      setEditingShow(show);
                    }}
                  >
                    编辑场次
                  </Button>
                  <Button size="small" onClick={() => setTierShowId(show.id)}>
                    添加票档
                  </Button>
                </Space>
              ) : null
            }
          >
            <Typography.Paragraph type="secondary">
              演出 {toDatetimeLocal(show.startAt).replace("T", " ")} ~ {toDatetimeLocal(show.endAt).replace("T", " ")}
              ；售卖 {toDatetimeLocal(show.saleStartAt).replace("T", " ")} ~ {toDatetimeLocal(show.saleEndAt).replace("T", " ")}
            </Typography.Paragraph>
            <Table
              rowKey="id"
              pagination={false}
              dataSource={show.tiers}
              columns={[
                { title: "票档", dataIndex: "name" },
                { title: "单价（元）", render: (_, row) => fenToYuan(row.unitPriceFen) },
                { title: "限购", dataIndex: "perUserLimit" },
                { title: "总量", dataIndex: "totalQty" },
                { title: "可售", dataIndex: "availableQty" },
                ...(canWrite && editable
                  ? [
                      {
                        title: "操作",
                        render: (_: unknown, row: TicketTier) => (
                          <Button
                            type="link"
                            size="small"
                            onClick={() => {
                              tierForm.reset({
                                name: row.name,
                                unitPriceFen: row.unitPriceFen,
                                perUserLimit: row.perUserLimit,
                                totalQty: row.totalQty
                              });
                              setEditingTier({ showId: show.id, tier: row });
                            }}
                          >
                            编辑
                          </Button>
                        )
                      }
                    ]
                  : [])
              ]}
            />
          </Card>
        ))}
      </Card>
      <Card title="审核记录">
        <Table
          rowKey="id"
          pagination={false}
          dataSource={activity?.audits ?? []}
          columns={[
            { title: "从", dataIndex: "fromStatus", render: (v: string) => reviewLabel(v || "") },
            { title: "到", dataIndex: "toStatus", render: (v: string) => reviewLabel(v) },
            { title: "意见", dataIndex: "comment" },
            { title: "时间", dataIndex: "createdAt" }
          ]}
        />
      </Card>
      <Modal
        title="添加场次"
        open={showOpen}
        onCancel={() => setShowOpen(false)}
        onOk={showForm.handleSubmit((v) => addShow.mutate(v))}
        confirmLoading={addShow.isPending}
        destroyOnClose
      >
        <ShowFields form={showForm} />
      </Modal>
      <Modal
        title="编辑场次"
        open={Boolean(editingShow)}
        onCancel={() => setEditingShow(null)}
        onOk={showForm.handleSubmit((v) => {
          if (editingShow) {
            updateShow.mutate({ ...v, id: editingShow.id });
          }
        })}
        confirmLoading={updateShow.isPending}
        destroyOnClose
      >
        <ShowFields form={showForm} />
      </Modal>
      <Modal
        title="添加票档"
        open={Boolean(tierShowId)}
        onCancel={() => setTierShowId(null)}
        onOk={tierForm.handleSubmit((v) => {
          if (tierShowId) {
            addTier.mutate({ ...v, showId: tierShowId });
          }
        })}
        confirmLoading={addTier.isPending}
        destroyOnClose
      >
        <TierFields form={tierForm} />
      </Modal>
      <Modal
        title="编辑票档"
        open={Boolean(editingTier)}
        onCancel={() => setEditingTier(null)}
        onOk={tierForm.handleSubmit((v) => {
          if (editingTier) {
            updateTier.mutate({ ...v, id: editingTier.tier.id });
          }
        })}
        confirmLoading={updateTier.isPending}
        destroyOnClose
      >
        <TierFields form={tierForm} />
      </Modal>
    </Space>
  );
}

function ShowFields({ form }: { form: ReturnType<typeof useForm<ShowForm>> }) {
  return (
    <Form layout="vertical">
      <Form.Item label="场次名称">
        <Controller name="name" control={form.control} render={({ field }) => <Input {...field} />} />
      </Form.Item>
      <Form.Item label="开演">
        <Controller name="startAt" control={form.control} render={({ field }) => <Input type="datetime-local" {...field} />} />
      </Form.Item>
      <Form.Item label="结束">
        <Controller name="endAt" control={form.control} render={({ field }) => <Input type="datetime-local" {...field} />} />
      </Form.Item>
      <Form.Item label="开售">
        <Controller
          name="saleStartAt"
          control={form.control}
          render={({ field }) => <Input type="datetime-local" {...field} />}
        />
      </Form.Item>
      <Form.Item label="停售">
        <Controller name="saleEndAt" control={form.control} render={({ field }) => <Input type="datetime-local" {...field} />} />
      </Form.Item>
    </Form>
  );
}

function TierFields({ form }: { form: ReturnType<typeof useForm<TierForm>> }) {
  return (
    <Form layout="vertical">
      <Form.Item label="名称">
        <Controller name="name" control={form.control} render={({ field }) => <Input {...field} />} />
      </Form.Item>
      <Form.Item label="单价（分）">
        <Controller
          name="unitPriceFen"
          control={form.control}
          render={({ field }) => <InputNumber {...field} min={0} style={{ width: "100%" }} />}
        />
      </Form.Item>
      <Form.Item label="每人限购">
        <Controller
          name="perUserLimit"
          control={form.control}
          render={({ field }) => <InputNumber {...field} min={1} style={{ width: "100%" }} />}
        />
      </Form.Item>
      <Form.Item label="库存总量">
        <Controller
          name="totalQty"
          control={form.control}
          render={({ field }) => <InputNumber {...field} min={1} style={{ width: "100%" }} />}
        />
      </Form.Item>
    </Form>
  );
}
