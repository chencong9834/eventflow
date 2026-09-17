import { Button, Layout, Menu, Typography } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { BrandMark } from "../shared/BrandMark";

export function PlatformLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <Layout className="admin-shell" style={{ minHeight: "100%" }}>
      <Layout.Sider breakpoint="lg" collapsedWidth={0}>
        <div style={{ padding: "20px 16px" }}>
          <BrandMark light />
          <Typography.Text style={{ color: "rgba(255,255,255,0.65)", display: "block", marginTop: 6 }}>
            平台运营
          </Typography.Text>
        </div>
        <Menu
          theme="dark"
          selectedKeys={[
            location.pathname.startsWith("/platform/tenants")
              ? "/platform/tenants"
              : location.pathname.startsWith("/platform/reviews")
                ? "/platform/reviews"
                : location.pathname.startsWith("/platform/orders")
                  ? "/platform/orders"
                  : location.pathname
          ]}
          onClick={(e) => navigate(e.key)}
          items={[
            { key: "/platform/home", label: "工作台" },
            { key: "/platform/tenants", label: "租户" },
            { key: "/platform/reviews", label: "活动审核" },
            { key: "/platform/orders", label: "跨租户订单" },
            { key: "/platform/notices", label: "通知" }
          ]}
        />
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
