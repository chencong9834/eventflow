import { Button, Card, Descriptions, Space, Table, Tag, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { http } from "../../shared/http";
import type { ApiResponse, TenantDetail } from "../../shared/types";

export function TenantDetailPage() {
  const { tenantId } = useParams();
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: ["platform", "tenants", tenantId],
    enabled: Boolean(tenantId),
    queryFn: async () => {
      const res = (await http.get(`/platform/tenants/${tenantId}`)) as ApiResponse<TenantDetail>;
      return res.data;
    }
  });

  if (query.isError) {
    return (
      <Card>
        <Typography.Text type="danger">加载失败</Typography.Text>
      </Card>
    );
  }

  const tenant = query.data;

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Button onClick={() => navigate("/platform/tenants")}>返回列表</Button>
      <Card loading={query.isLoading} title={tenant?.name}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="编码">{tenant?.tenantCode}</Descriptions.Item>
          <Descriptions.Item label="类型">{tenant?.type}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={tenant?.status === "ACTIVE" ? "green" : "red"}>
              {tenant?.status === "ACTIVE" ? "启用" : "停用"}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="ID">{tenant?.id}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="角色">
        <Table
          rowKey="id"
          pagination={false}
          dataSource={tenant?.roles ?? []}
          columns={[
            { title: "名称", dataIndex: "name", width: 160 },
            { title: "编码", dataIndex: "code", width: 220 },
            {
              title: "权限",
              dataIndex: "permissions",
              render: (perms: string[]) => (perms ?? []).join("，")
            }
          ]}
        />
      </Card>
      <Card title="账号">
        <Table
          rowKey="id"
          pagination={false}
          dataSource={tenant?.users ?? []}
          columns={[
            { title: "用户名", dataIndex: "username" },
            { title: "姓名", dataIndex: "displayName" },
            { title: "角色", dataIndex: "roleName" },
            { title: "状态", dataIndex: "status" }
          ]}
        />
      </Card>
    </Space>
  );
}
