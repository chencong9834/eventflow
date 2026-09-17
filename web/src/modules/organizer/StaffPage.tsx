import { Button, Card, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { http, ApiError } from "../../shared/http";
import type { ApiResponse, StaffRole, StaffUser } from "../../shared/types";

const resetSchema = z.object({
  password: z.string().min(8, "密码至少 8 位").max(72, "密码过长")
});

const schema = z.object({
  username: z
    .string()
    .trim()
    .regex(/^[a-z][a-z0-9_]{2,63}$/, "用户名小写字母开头，仅字母数字下划线，至少 3 位"),
  password: z.string().min(8, "密码至少 8 位").max(72, "密码过长"),
  displayName: z.string().trim().min(1, "请输入姓名").max(128),
  roleCode: z.string().min(1, "请选择角色")
});

type FormValues = z.infer<typeof schema>;

export function StaffPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [resetId, setResetId] = useState<string | null>(null);
  const staff = useQuery({
    queryKey: ["organizer", "staff"],
    queryFn: async () => {
      const res = (await http.get("/organizer/staff")) as ApiResponse<StaffUser[]>;
      return res.data;
    }
  });
  const roles = useQuery({
    queryKey: ["organizer", "roles"],
    queryFn: async () => {
      const res = (await http.get("/organizer/roles")) as ApiResponse<StaffRole[]>;
      return res.data;
    }
  });
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", password: "", displayName: "", roleCode: "ORGANIZER_OPERATOR" }
  });
  const resetForm = useForm<z.infer<typeof resetSchema>>({
    resolver: zodResolver(resetSchema),
    defaultValues: { password: "" }
  });
  const create = useMutation({
    mutationFn: async (values: FormValues) => {
      await http.post("/organizer/staff", values);
    },
    onSuccess: async () => {
      message.success("已创建账号，可立即登录");
      setOpen(false);
      form.reset();
      await queryClient.invalidateQueries({ queryKey: ["organizer", "staff"] });
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "创建失败")
  });
  const patchStaff = useMutation({
    mutationFn: async (payload: { id: string; body: Record<string, string> }) => {
      await http.patch(`/organizer/staff/${payload.id}`, payload.body);
    },
    onSuccess: async () => {
      message.success("已更新");
      await queryClient.invalidateQueries({ queryKey: ["organizer", "staff"] });
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "操作失败")
  });

  return (
    <Card>
      <Space style={{ width: "100%", justifyContent: "space-between", marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          员工账号
        </Typography.Title>
        <Button type="primary" onClick={() => setOpen(true)}>
          新建账号
        </Button>
      </Space>
      <Typography.Paragraph type="secondary">
        只能管理本主办方租户内账号。至少保留一名启用中的管理员；停用或改密会使对方重新登录。
      </Typography.Paragraph>
      <Table
        rowKey="id"
        loading={staff.isLoading}
        dataSource={staff.data ?? []}
        columns={[
          { title: "用户名", dataIndex: "username" },
          { title: "姓名", dataIndex: "displayName" },
          { title: "角色", dataIndex: "roleName" },
          {
            title: "状态",
            dataIndex: "status",
            render: (status: string) => (
              <Tag color={status === "ACTIVE" ? "green" : "red"}>{status === "ACTIVE" ? "启用" : "停用"}</Tag>
            )
          },
          {
            title: "操作",
            width: 360,
            render: (_, row) => (
              <Space>
                <Select
                  size="small"
                  style={{ width: 140 }}
                  value={row.roleCode}
                  options={(roles.data ?? []).map((role) => ({ value: role.code, label: role.name }))}
                  onChange={(roleCode) => patchStaff.mutate({ id: row.id, body: { roleCode } })}
                />
                <Button type="link" onClick={() => setResetId(row.id)}>
                  重置密码
                </Button>
                <Button
                  type="link"
                  danger={row.status === "ACTIVE"}
                  onClick={() =>
                    patchStaff.mutate({
                      id: row.id,
                      body: { status: row.status === "ACTIVE" ? "DISABLED" : "ACTIVE" }
                    })
                  }
                >
                  {row.status === "ACTIVE" ? "停用" : "启用"}
                </Button>
              </Space>
            )
          }
        ]}
      />
      <Modal
        title="新建账号"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={form.handleSubmit((values) => create.mutate(values))}
        confirmLoading={create.isPending}
        destroyOnClose
      >
        <Form layout="vertical">
          <Form.Item
            label="用户名"
            validateStatus={form.formState.errors.username ? "error" : ""}
            help={form.formState.errors.username?.message}
          >
            <Controller name="username" control={form.control} render={({ field }) => <Input {...field} />} />
          </Form.Item>
          <Form.Item
            label="密码"
            validateStatus={form.formState.errors.password ? "error" : ""}
            help={form.formState.errors.password?.message}
          >
            <Controller
              name="password"
              control={form.control}
              render={({ field }) => <Input.Password {...field} autoComplete="new-password" />}
            />
          </Form.Item>
          <Form.Item
            label="姓名"
            validateStatus={form.formState.errors.displayName ? "error" : ""}
            help={form.formState.errors.displayName?.message}
          >
            <Controller name="displayName" control={form.control} render={({ field }) => <Input {...field} />} />
          </Form.Item>
          <Form.Item label="角色">
            <Controller
              name="roleCode"
              control={form.control}
              render={({ field }) => (
                <Select
                  {...field}
                  options={(roles.data ?? []).map((role) => ({ value: role.code, label: role.name }))}
                />
              )}
            />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="重置密码"
        open={Boolean(resetId)}
        onCancel={() => {
          setResetId(null);
          resetForm.reset();
        }}
        onOk={resetForm.handleSubmit((values) => {
          if (!resetId) {
            return;
          }
          patchStaff.mutate(
            { id: resetId, body: { password: values.password } },
            {
              onSuccess: () => {
                setResetId(null);
                resetForm.reset();
              }
            }
          );
        })}
        confirmLoading={patchStaff.isPending}
        destroyOnClose
      >
        <Form layout="vertical">
          <Form.Item
            label="新密码"
            validateStatus={resetForm.formState.errors.password ? "error" : ""}
            help={resetForm.formState.errors.password?.message}
          >
            <Controller
              name="password"
              control={resetForm.control}
              render={({ field }) => <Input.Password {...field} autoComplete="new-password" />}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
