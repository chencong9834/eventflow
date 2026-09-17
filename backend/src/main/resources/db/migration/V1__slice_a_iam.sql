CREATE TABLE sys_tenant (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_sys_tenant_code UNIQUE (tenant_code)
);

CREATE TABLE sys_role (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_sys_role_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE sys_permission (
  id BIGINT NOT NULL PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_sys_permission_code UNIQUE (code)
);

CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE sys_user (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  mobile VARCHAR(32) NULL,
  role_code VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE INDEX idx_sys_user_tenant ON sys_user (tenant_id);
CREATE INDEX idx_sys_role_tenant ON sys_role (tenant_id);

INSERT INTO sys_tenant (id, tenant_code, name, type, status, created_by) VALUES
  (1001, 'platform', 'EventFlow 平台', 'PLATFORM', 'ACTIVE', 0),
  (1002, 'buyer', 'EventFlow 购票用户', 'BUYER', 'ACTIVE', 0),
  (1003, 'org-demo', '示例主办方', 'ORGANIZER', 'ACTIVE', 0);

INSERT INTO sys_role (id, tenant_id, code, name, created_by) VALUES
  (3001, 1001, 'PLATFORM', '平台运营', 0),
  (3002, 1003, 'ORGANIZER', '主办方', 0),
  (3003, 1002, 'BUYER', '购票用户', 0);

INSERT INTO sys_permission (id, code, name, created_by) VALUES
  (4001, 'tenant:read', '查看租户', 0),
  (4002, 'review:write', '审核活动', 0),
  (4003, 'report:read', '查看报表', 0),
  (4004, 'activity:write', '维护活动', 0),
  (4005, 'inventory:write', '维护库存', 0),
  (4006, 'order:read', '查看订单', 0),
  (4007, 'ticket:verify', '核销票券', 0),
  (4008, 'refund:write', '发起退款', 0),
  (4009, 'catalog:read', '浏览在售活动', 0),
  (4010, 'order:write', '下单', 0),
  (4011, 'ticket:read', '查看我的票', 0);

INSERT INTO sys_role_permission (role_id, permission_id, tenant_id) VALUES
  (3001, 4001, 1001),
  (3001, 4002, 1001),
  (3001, 4003, 1001),
  (3002, 4003, 1003),
  (3002, 4004, 1003),
  (3002, 4005, 1003),
  (3002, 4006, 1003),
  (3002, 4007, 1003),
  (3002, 4008, 1003),
  (3003, 4009, 1002),
  (3003, 4010, 1002),
  (3003, 4011, 1002);

-- password_hash SEED_PLAIN is replaced with BCrypt on first boot
INSERT INTO sys_user (id, tenant_id, username, password_hash, display_name, mobile, role_code, status, created_by) VALUES
  (2001, 1001, 'platform', 'SEED_PLAIN', '平台运营', '13800000001', 'PLATFORM', 'ACTIVE', 0),
  (2002, 1003, 'organizer', 'SEED_PLAIN', '主办方管理员', '13800000002', 'ORGANIZER', 'ACTIVE', 0),
  (2003, 1002, 'buyer', 'SEED_PLAIN', '购票用户', '13800000003', 'BUYER', 'ACTIVE', 0);
