import { Button, Layout, Menu, Typography } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { BrandMark } from "../shared/BrandMark";

export function OrganizerLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const perms = user?.permissions ?? [];
  const items = [
    { key: "/organizer/home", label: "工作台" },
    perms.includes("user:write") ? { key: "/organizer/staff", label: "员工账号" } : null,
    perms.some((p) =>
      ["activity:write", "order:read", "ticket:verify", "report:read", "refund:write"].includes(p)
    )
      ? { key: "/organizer/activities", label: "活动" }
      : null,
    perms.includes("order:read") ? { key: "/organizer/orders", label: "订单" } : null,
    perms.includes("ticket:verify") ? { key: "/organizer/checkin", label: "核销" } : null,
    perms.includes("report:read") ? { key: "/organizer/reports", label: "报表" } : null,
    { key: "/organizer/notices", label: "通知" }
  ].filter((item): item is { key: string; label: string } => Boolean(item));
  const selected = location.pathname.startsWith("/organizer/activities")
    ? "/organizer/activities"
    : location.pathname.startsWith("/organizer/staff")
      ? "/organizer/staff"
      : location.pathname.startsWith("/organizer/orders")
        ? "/organizer/orders"
        : location.pathname;

  return (
    <Layout className="admin-shell" style={{ minHeight: "100%" }}>
      <Layout.Sider
        theme="light"
        breakpoint="lg"
        collapsedWidth={0}
        style={{ borderRight: "1px solid #eef0f4" }}
      >
        <div style={{ padding: "20px 16px" }}>
          <BrandMark />
          <Typography.Text type="secondary" style={{ display: "block", marginTop: 6 }}>
            主办方后台
          </Typography.Text>
        </div>
        <Menu selectedKeys={[selected]} onClick={(e) => navigate(e.key)} items={items} />
      </Layout.Sider>
      <Layout>
        <Layout.Header
          style={{
            background: "#fff",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            paddingInline: 24
          }}
        >
          <Typography.Text>
            {user?.displayName} · {user?.tenantName}
          </Typography.Text>
          <Button
            onClick={() => {
              logout();
              navigate("/login");
            }}
          >
            退出
          </Button>
        </Layout.Header>
        <Layout.Content className="admin-content" style={{ padding: 24 }}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
