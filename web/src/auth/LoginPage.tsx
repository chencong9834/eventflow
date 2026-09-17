import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Card, Input, Space, Spin, Typography, message } from "antd";
import { Navigate, useNavigate } from "react-router-dom";
import { homeByTenantType, useAuth } from "./AuthProvider";
import { loginSchema, type LoginFormValues } from "./loginSchema";
import { ApiError } from "../shared/http";
import { BrandMark, DEMO_ACCOUNTS } from "../shared/BrandMark";

export function LoginPage() {
  const { ready, token, user, login } = useAuth();
  const navigate = useNavigate();
  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting }
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "buyer", password: "Passw0rd!" }
  });

  if (!ready) {
    return <Spin style={{ display: "block", margin: "20vh auto" }} />;
  }

  if (token && user) {
    return <Navigate to={homeByTenantType(user.tenantType)} replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    try {
      const logged = await login(values.username, values.password);
      message.success("登录成功");
      navigate(homeByTenantType(logged.tenantType), { replace: true });
    } catch (e) {
      message.error(e instanceof ApiError ? e.message : "登录失败");
    }
  });

  return (
    <div className="login-page">
      <section className="login-hero">
        <BrandMark light />
        <Typography.Title style={{ color: "#fff", marginTop: 24, maxWidth: 420 }}>活动售票演示</Typography.Title>
        <Typography.Paragraph style={{ color: "rgba(255,255,255,0.78)", maxWidth: 440, fontSize: 16 }}>
          多租户隔离、平台审核、Redis 预扣库存、模拟支付出票。建议按 平台审核 → 购票支付 → 主办方核销 走一遍。
        </Typography.Paragraph>
      </section>
      <section className="login-panel">
        <Card className="login-card" styles={{ body: { padding: 28 } }}>
          <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 4 }}>
            登录
          </Typography.Title>
          <Typography.Paragraph type="secondary">三个演示账号同一密码，点选即可填入。</Typography.Paragraph>
          <form onSubmit={onSubmit}>
            <Typography.Text>用户名</Typography.Text>
            <Controller
              name="username"
              control={control}
              render={({ field }) => (
                <Input {...field} size="large" autoComplete="username" style={{ margin: "8px 0 4px" }} />
              )}
            />
            <Typography.Text type="danger">{errors.username?.message}</Typography.Text>
            <div style={{ marginTop: 12 }}>
              <Typography.Text>密码</Typography.Text>
              <Controller
                name="password"
                control={control}
                render={({ field }) => (
                  <Input.Password {...field} size="large" autoComplete="current-password" style={{ margin: "8px 0 4px" }} />
                )}
              />
              <Typography.Text type="danger">{errors.password?.message}</Typography.Text>
            </div>
            <Button type="primary" htmlType="submit" size="large" block loading={isSubmitting} style={{ marginTop: 16 }}>
              进入工作台
            </Button>
          </form>
          <Space direction="vertical" size={8} style={{ width: "100%", marginTop: 20 }}>
            {DEMO_ACCOUNTS.map((account) => (
              <button
                key={account.username}
                type="button"
                className="demo-account"
                onClick={() => {
                  setValue("username", account.username);
                  setValue("password", account.password);
                }}
              >
                <Typography.Text strong>
                  {account.role} · {account.username}
                </Typography.Text>
                <br />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {account.hint} · 密码 {account.password}
                </Typography.Text>
              </button>
            ))}
          </Space>
        </Card>
      </section>
    </div>
  );
}
