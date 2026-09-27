export type TenantType = "PLATFORM" | "ORGANIZER" | "BUYER";

export interface CurrentUser {
  userId: number;
  username: string;
  displayName: string;
  tenantId: number;
  tenantCode: string;
  tenantName: string;
  tenantType: TenantType;
  roleCode: string;
  roleName: string;
  permissions: string[];
}

export interface LoginPayload {
  token: string;
  user: CurrentUser;
}

export interface Tenant {
  id: string;
  tenantCode: string;
  name: string;
  type: string;
  status: string;
  userCount: number;
  roleCount: number;
}

export interface TenantRole {
  id: string;
  code: string;
  name: string;
  permissions: string[];
}

export interface TenantUser {
  id: string;
  username: string;
  displayName: string;
  mobile?: string;
  roleCode: string;
  roleName: string;
  status: string;
}

export interface TenantDetail extends Tenant {
  roles: TenantRole[];
  users: TenantUser[];
}

export interface PlatformOverview {
  tenantTotal: number;
  organizerActive: number;
  organizerDisabled: number;
  userTotal: number;
  pendingReviewHint: number;
}

export interface StaffUser {
  id: string;
  username: string;
  displayName: string;
  mobile?: string;
  roleCode: string;
  roleName: string;
  status: string;
}

export interface StaffRole {
  id: string;
  code: string;
  name: string;
  permissions: string[];
}

export interface ActivitySummary {
  id: string;
  tenantId: string;
  title: string;
  reviewStatus: string;
  saleStatus: string;
  showCount: number;
  updatedAt?: string;
  organizerName?: string;
  organizerCode?: string;
  description?: string;
  coverUrl?: string;
}

export interface TicketTier {
  id: string;
  name: string;
  unitPriceFen: number;
  perUserLimit: number;
  totalQty: number;
  availableQty: number;
  reservedQty: number;
  soldQty: number;
}

export interface ActivityShow {
  id: string;
  name: string;
  startAt: string;
  endAt: string;
  saleStartAt: string;
  saleEndAt: string;
  tiers: TicketTier[];
}

export interface AuditLog {
  id: string;
  actorUserId: string;
  fromStatus?: string;
  toStatus: string;
  comment?: string;
  createdAt?: string;
}

export interface ActivityDetail extends ActivitySummary {
  description?: string;
  coverUrl?: string;
  createdAt?: string;
  shows: ActivityShow[];
  audits: AuditLog[];
}

export interface Order {
  id: string;
  orderNo: string;
  tenantId: string;
  activityTitle: string;
  showName: string;
  tierName: string;
  qty: number;
  unitPriceFen: number;
  amountFen: number;
  status: string;
  payDeadlineAt?: string;
  createdAt?: string;
  tickets: IssuedTicket[];
}

export interface IssuedTicket {
  id: string;
  ticketNo: string;
  verifyCode: string;
  status: string;
  usedAt?: string;
}

export interface ReportSummary {
  from: string;
  to: string;
  paidOrderCount: number;
  soldQty: number;
  salesFen: number;
  refundFen: number;
  usedTicketCount: number;
}

export interface SiteNotice {
  id: string;
  title: string;
  body: string;
  createdAt?: string;
}

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}
