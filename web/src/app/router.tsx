import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "../auth/LoginPage";
import { PlatformLayout } from "../layouts/PlatformLayout";
import { OrganizerLayout } from "../layouts/OrganizerLayout";
import { BuyerLayout } from "../layouts/BuyerLayout";
import { PlatformHome } from "../modules/platform/PlatformHome";
import { TenantList } from "../modules/platform/TenantList";
import { TenantDetailPage } from "../modules/platform/TenantDetailPage";
import { ReviewListPage } from "../modules/platform/ReviewListPage";
import { ReviewDetailPage } from "../modules/platform/ReviewDetailPage";
import { OrganizerHome } from "../modules/activity/OrganizerHome";
import { ActivityListPage } from "../modules/activity/ActivityListPage";
import { ActivityDetailPage } from "../modules/activity/ActivityDetailPage";
import { StaffPage } from "../modules/organizer/StaffPage";
import { CatalogPage } from "../modules/buyer/CatalogPage";
import { CatalogDetailPage } from "../modules/buyer/CatalogDetailPage";
import { BuyerOrdersPage } from "../modules/buyer/BuyerOrdersPage";
import { BuyerOrderDetailPage } from "../modules/buyer/BuyerOrderDetailPage";
import { BuyerTicketsPage } from "../modules/buyer/BuyerTicketsPage";
import { NoticesPage } from "../modules/shared/NoticesPage";
import { OrganizerOrdersPage } from "../modules/order/OrganizerOrdersPage";
import { OrganizerOrderDetailPage, PlatformOrderDetailPage } from "../modules/order/AdminOrderDetailPage";
import { CheckinPage } from "../modules/ticket/CheckinPage";
import { ReportPage } from "../modules/reporting/ReportPage";
import { PlatformOrdersPage } from "../modules/platform/PlatformOrdersPage";
import { RequireAuth } from "./RequireAuth";
import { RequirePermission } from "./RequirePermission";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <RequireAuth tenantTypes={["PLATFORM"]} />,
    children: [
      {
        path: "/platform",
        element: <PlatformLayout />,
        children: [
          { index: true, element: <Navigate to="home" replace /> },
          { path: "home", element: <PlatformHome /> },
          { path: "tenants", element: <TenantList /> },
          { path: "tenants/:tenantId", element: <TenantDetailPage /> },
          { path: "reviews", element: <ReviewListPage /> },
          { path: "reviews/:activityId", element: <ReviewDetailPage /> },
          {
            element: <RequirePermission anyOf={["order:read", "refund:write"]} />,
            children: [
              { path: "orders", element: <PlatformOrdersPage /> },
              { path: "orders/:orderId", element: <PlatformOrderDetailPage /> }
            ]
          },
          { path: "notices", element: <NoticesPage /> }
        ]
      }
    ]
  },
  {
    element: <RequireAuth tenantTypes={["ORGANIZER"]} />,
    children: [
      {
        path: "/organizer",
        element: <OrganizerLayout />,
        children: [
          { index: true, element: <Navigate to="home" replace /> },
          { path: "home", element: <OrganizerHome /> },
          { path: "notices", element: <NoticesPage /> },
          {
            element: <RequirePermission anyOf={["user:write"]} />,
            children: [{ path: "staff", element: <StaffPage /> }]
          },
          {
            element: (
              <RequirePermission
                anyOf={["activity:write", "order:read", "ticket:verify", "report:read", "refund:write"]}
              />
            ),
            children: [
              { path: "activities", element: <ActivityListPage /> },
              { path: "activities/:activityId", element: <ActivityDetailPage /> }
            ]
          },
          {
            element: <RequirePermission anyOf={["order:read", "refund:write"]} />,
            children: [
              { path: "orders", element: <OrganizerOrdersPage /> },
              { path: "orders/:orderId", element: <OrganizerOrderDetailPage /> }
            ]
          },
          {
            element: <RequirePermission anyOf={["ticket:verify"]} />,
            children: [{ path: "checkin", element: <CheckinPage /> }]
          },
          {
            element: <RequirePermission anyOf={["report:read"]} />,
            children: [{ path: "reports", element: <ReportPage /> }]
          }
        ]
      }
    ]
  },
  {
    element: <RequireAuth tenantTypes={["BUYER"]} />,
    children: [
      {
        path: "/buyer",
        element: <BuyerLayout />,
        children: [
          { index: true, element: <Navigate to="home" replace /> },
          { path: "home", element: <CatalogPage /> },
          { path: "activities/:activityId", element: <CatalogDetailPage /> },
          { path: "orders", element: <BuyerOrdersPage /> },
          { path: "orders/:orderId", element: <BuyerOrderDetailPage /> },
          { path: "tickets", element: <BuyerTicketsPage /> },
          { path: "notices", element: <NoticesPage /> }
        ]
      }
    ]
  },
  { path: "/", element: <Navigate to="/login" replace /> },
  { path: "*", element: <Navigate to="/login" replace /> }
]);
