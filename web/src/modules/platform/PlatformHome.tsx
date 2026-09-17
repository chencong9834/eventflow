import { Card, Col, Row, Statistic, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthProvider";
import { http } from "../../shared/http";
import type { ApiResponse, PlatformOverview } from "../../shared/types";

export function PlatformHome() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const overview = useQuery({
    queryKey: ["platform", "overview"],
    queryFn: async () => {
      const res = (await http.get("/platform/overview")) as ApiResponse<PlatformOverview>;
      return res.data;
    }
  });

  return (
    <div>
      <Typography.Title level={3}>平台工作台</Typography.Title>
      <Typography.Paragraph>
        你在内置平台租户下，负责主办方入驻、活动审核，以及限定时间范围内的跨租户订单与客服整单退款。
      </Typography.Paragraph>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card hoverable onClick={() => navigate("/platform/tenants")}>
            <Statistic title="租户总数" value={overview.data?.tenantTotal ?? "-"} loading={overview.isLoading} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="在营主办方" value={overview.data?.organizerActive ?? "-"} loading={overview.isLoading} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="已停用主办方" value={overview.data?.organizerDisabled ?? "-"} loading={overview.isLoading} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable onClick={() => navigate("/platform/reviews")}>
            <Statistic
              title="待审活动"
              value={overview.data?.pendingReviewHint ?? "-"}
              loading={overview.isLoading}
            />
          </Card>
        </Col>
      </Row>
      <Card title="当前账号">
        <Typography.Paragraph style={{ marginBottom: 0 }}>
          {user?.displayName} · {user?.roleName} · 权限：{(user?.permissions ?? []).join("，")}
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
