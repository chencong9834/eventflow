import { Card, Descriptions, Typography } from "antd";
import { useAuth } from "../../auth/AuthProvider";

export function OrganizerHome() {
  const { user } = useAuth();
  return (
    <Card>
      <Typography.Title level={3}>主办方工作台</Typography.Title>
      <Typography.Paragraph>
        可维护本租户员工、活动、订单、核销与报表。购票用户在公共目录下单，支付成功后出票。
      </Typography.Paragraph>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="租户">
          {user?.tenantName}（{user?.tenantId}）
        </Descriptions.Item>
        <Descriptions.Item label="角色">{user?.roleName}</Descriptions.Item>
        <Descriptions.Item label="权限">{(user?.permissions ?? []).join("，")}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}
