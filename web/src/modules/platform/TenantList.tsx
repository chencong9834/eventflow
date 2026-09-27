import { Button, Card, Form, Input, Modal, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { http, ApiError } from "../../shared/http";
import { listPagination } from "../../shared/page";
import type { ApiResponse, PageResult, Tenant } from "../../shared/types";

const schema = z.object({
  tenantCode: z
    .string()
    .trim()
    .regex(/^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$/, "编码须为小写字母、数字和中划线，至少 3 位"),
  name: z.string().trim().min(1, "请输入名称").max(128, "名称过长"),
  adminUsername: z
    .string()
    .trim()
    .regex(/^[a-z][a-z0-9_]{2,63}$/, "用户名小写字母开头，仅字母数字下划线，至少 3 位"),
  adminPassword: z.string().min(8, "密码至少 8 位").max(72, "密码过长"),
  adminDisplayName: z.string().trim().min(1, "请输入管理员姓名").max(128, "过长")
});

type FormValues = z.infer<typeof schema>;

export function TenantList() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const query = useQuery({
    queryKey: ["platform", "tenants", page, size],
    queryFn: async () => {
      const res = (await http.get("/platform/tenants", { params: { page, size } })) as ApiResponse<PageResult<Tenant>>;
      return res.data;
    }
  });
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      tenantCode: "",
      name: "",
      adminUsername: "",
      adminPassword: "",
      adminDisplayName: ""
    }
  });
  const create = useMutation({
    mutationFn: async (values: FormValues) => {
      const res = (await http.post("/platform/tenants", values)) as ApiResponse<Tenant>;
      return res.data;
    },
    onSuccess: async (tenant) => {
      message.success(`已创建 ${tenant.name}，管理员可立即登录`);
      setOpen(false);
      form.reset();
      await queryClient.invalidateQueries({ queryKey: ["platform", "tenants"] });
    },
    onError: (e) => {
      message.error(e instanceof ApiError ? e.message : "创建失败");
    }
  });
  const toggle = useMutation({
    mutationFn: async (row: Tenant) => {
      const status = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
      await http.patch(`/platform/tenants/${row.id}/status`, { status });
    },
    onSuccess: async () => {
      message.success("已更新租户状态");
      await queryClient.invalidateQueries({ queryKey: ["platform", "tenants"] });
    },
    onError: (e) => {
      message.error(e instanceof ApiError ? e.message : "操作失败");
    }
  });

  if (query.isError) {
    return (
      <Card>
        <Typography.Text type="danger">
          {query.error instanceof Error ? query.error.message : "加载失败"}
        </Typography.Text>
      </Card>
    );
  }

  return (
    <Card>
      <Space style={{ width: "100%", justifyContent: "space-between", marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          租户治理
        </Typography.Title>
        <Button type="primary" onClick={() => setOpen(true)}>
          新建主办方
        </Button>
      </Space>
      <Typography.Paragraph type="secondary">
        平台租户与购票租户为内置，不可停用。新建主办方会复制四套角色并开通首位管理员。
      </Typography.Paragraph>
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.items ?? []}
        pagination={listPagination(page, size, query.data?.total ?? 0, (nextPage, nextSize) => {
          setPage(nextPage);
          setSize(nextSize);
        })}
        columns={[
          { title: "编码", dataIndex: "tenantCode", width: 140 },
          { title: "名称", dataIndex: "name" },
          { title: "类型", dataIndex: "type", width: 120 },
          {
            title: "状态",
            dataIndex: "status",
            width: 100,
            render: (status: string) => (
              <Tag color={status === "ACTIVE" ? "green" : "red"}>{status === "ACTIVE" ? "启用" : "停用"}</Tag>
            )
          },
          { title: "账号数", dataIndex: "userCount", width: 90 },
          { title: "角色数", dataIndex: "roleCount", width: 90 },
          {
            title: "操作",
            width: 200,
            render: (_, row) => (
              <Space>
                <Button type="link" onClick={() => navigate(`/platform/tenants/${row.id}`)}>
                  详情
                </Button>
                {row.type === "ORGANIZER" ? (
                  <Button type="link" danger={row.status === "ACTIVE"} onClick={() => toggle.mutate(row)}>
                    {row.status === "ACTIVE" ? "停用" : "启用"}
                  </Button>
                ) : null}
              </Space>
            )
          }
        ]}
      />
      <Modal
        title="新建主办方"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={form.handleSubmit((values) => create.mutate(values))}
        confirmLoading={create.isPending}
        destroyOnClose
        width={520}
      >
        <Form layout="vertical">
          <Form.Item
            label="租户编码"
            validateStatus={form.formState.errors.tenantCode ? "error" : ""}
            help={form.formState.errors.tenantCode?.message}
          >
            <Controller
              name="tenantCode"
              control={form.control}
              render={({ field }) => <Input {...field} placeholder="org-example" />}
            />
          </Form.Item>
          <Form.Item
            label="租户名称"
            validateStatus={form.formState.errors.name ? "error" : ""}
            help={form.formState.errors.name?.message}
          >
            <Controller name="name" control={form.control} render={({ field }) => <Input {...field} />} />
          </Form.Item>
          <Form.Item
            label="管理员用户名"
            validateStatus={form.formState.errors.adminUsername ? "error" : ""}
            help={form.formState.errors.adminUsername?.message}
          >
            <Controller
              name="adminUsername"
              control={form.control}
              render={({ field }) => <Input {...field} placeholder="acme_admin" autoComplete="off" />}
            />
          </Form.Item>
          <Form.Item
            label="管理员密码"
            validateStatus={form.formState.errors.adminPassword ? "error" : ""}
            help={form.formState.errors.adminPassword?.message}
          >
            <Controller
              name="adminPassword"
              control={form.control}
              render={({ field }) => <Input.Password {...field} autoComplete="new-password" />}
            />
          </Form.Item>
          <Form.Item
            label="管理员姓名"
            validateStatus={form.formState.errors.adminDisplayName ? "error" : ""}
            help={form.formState.errors.adminDisplayName?.message}
          >
            <Controller name="adminDisplayName" control={form.control} render={({ field }) => <Input {...field} />} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
