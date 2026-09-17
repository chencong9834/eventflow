import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";

export function RequirePermission({ anyOf }: { anyOf: string[] }) {
  const { user } = useAuth();
  const granted = user?.permissions ?? [];
  if (!anyOf.some((item) => granted.includes(item))) {
    const home =
      user?.tenantType === "PLATFORM"
        ? "/platform/home"
        : user?.tenantType === "BUYER"
          ? "/buyer/home"
          : "/organizer/home";
    return <Navigate to={home} replace />;
  }
  return <Outlet />;
}
