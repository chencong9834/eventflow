import { Button, Layout, Menu, Typography } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { BrandMark } from "../shared/BrandMark";

export function BuyerLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const selected = location.pathname.startsWith("/buyer/orders")
    ? "/buyer/orders"
    : location.pathname.startsWith("/buyer/tickets")
      ? "/buyer/tickets"
      : location.pathname.startsWith("/buyer/notices")
        ? "/buyer/notices"
        : "/buyer/home";

  return (
    <Layout style={{ minHeight: "100%" }}>
      <Layout.Header
        className="buyer-header"
        style={{
          background: "#0f172a",
          display: "flex",
          alignItems: "center",
          gap: 12,
          paddingInline: 16
        }}
      >
        <button type="button" onClick={() => navigate("/buyer/home")} style={{ background: "none", border: 0, padding: 0, cursor: "pointer" }}>
          <BrandMark light />
        </button>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[selected]}
          onClick={(e) => navigate(e.key)}
          overflowedIndicator="更多"
          items={[
            { key: "/buyer/home", label: "目录" },
            { key: "/buyer/orders", label: "订单" },
            { key: "/buyer/tickets", label: "我的票" },
            { key: "/buyer/notices", label: "通知" }
          ]}
          style={{ flex: 1, minWidth: 0, background: "transparent" }}
        />
        <Typography.Text style={{ color: "rgba(255,255,255,0.75)", whiteSpace: "nowrap" }} ellipsis>
          {user?.displayName}
        </Typography.Text>
        <Button
          ghost
          onClick={() => {
            logout();
            navigate("/login");
          }}
        >
          退出
        </Button>
      </Layout.Header>
      <Layout.Content style={{ padding: "24px 16px 48px", maxWidth: 1080, margin: "0 auto", width: "100%" }}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
