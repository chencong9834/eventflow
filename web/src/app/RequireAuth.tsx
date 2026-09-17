import { Spin } from "antd";
import { Navigate, Outlet } from "react-router-dom";
import { homeByTenantType, useAuth } from "../auth/AuthProvider";
import type { TenantType } from "../shared/types";

export function RequireAuth({ tenantTypes }: { tenantTypes?: TenantType[] }) {
  const { ready, token, user } = useAuth();
  if (!ready) {
    return <Spin style={{ display: "block", margin: "20vh auto" }} />;
  }
  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }
  if (tenantTypes && !tenantTypes.includes(user.tenantType)) {
    return <Navigate to={homeByTenantType(user.tenantType)} replace />;
  }
  return <Outlet />;
}
