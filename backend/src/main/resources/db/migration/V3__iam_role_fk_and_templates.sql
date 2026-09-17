INSERT INTO sys_permission (id, code, name, created_by) VALUES
  (4012, 'tenant:write', '维护租户', 0);

INSERT INTO sys_role_permission (role_id, permission_id, tenant_id) VALUES
  (3001, 4012, 1001);

CREATE TABLE sys_role_template (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_type VARCHAR(32) NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_sys_role_template_type_code UNIQUE (tenant_type, code),
  CONSTRAINT chk_sys_role_template_type CHECK (tenant_type IN ('PLATFORM', 'ORGANIZER', 'BUYER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_role_template_permission (
  template_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (template_id, permission_id),
  CONSTRAINT fk_rtp_template FOREIGN KEY (template_id) REFERENCES sys_role_template (id),
  CONSTRAINT fk_rtp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_role_template (id, tenant_type, code, name, created_by) VALUES
  (3101, 'PLATFORM', 'PLATFORM', '平台运营', 0),
  (3102, 'ORGANIZER', 'ORGANIZER', '主办方', 0),
  (3103, 'BUYER', 'BUYER', '购票用户', 0);

INSERT INTO sys_role_template_permission (template_id, permission_id) VALUES
  (3101, 4001),
  (3101, 4012),
  (3101, 4002),
  (3101, 4003),
  (3102, 4003),
  (3102, 4004),
  (3102, 4005),
  (3102, 4006),
  (3102, 4007),
  (3102, 4008),
  (3103, 4009),
  (3103, 4010),
  (3103, 4011);

ALTER TABLE sys_user
  ADD COLUMN role_id BIGINT NULL AFTER role_code;

UPDATE sys_user u
  INNER JOIN sys_role r ON r.tenant_id = u.tenant_id AND r.code = u.role_code
  SET u.role_id = r.id;

ALTER TABLE sys_user
  MODIFY COLUMN role_id BIGINT NOT NULL,
  DROP COLUMN role_code;

ALTER TABLE sys_tenant
  MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  ADD CONSTRAINT chk_sys_tenant_type CHECK (type IN ('PLATFORM', 'ORGANIZER', 'BUYER')),
  ADD CONSTRAINT chk_sys_tenant_status CHECK (status IN ('ACTIVE', 'DISABLED'));

ALTER TABLE sys_user
  MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  ADD CONSTRAINT chk_sys_user_status CHECK (status IN ('ACTIVE', 'DISABLED'));

ALTER TABLE sys_role
  MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

ALTER TABLE sys_permission
  MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

ALTER TABLE sys_role
  ADD CONSTRAINT fk_sys_role_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id);

ALTER TABLE sys_role_permission
  ADD CONSTRAINT fk_srp_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  ADD CONSTRAINT fk_srp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id),
  ADD CONSTRAINT fk_srp_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id);

ALTER TABLE sys_user
  ADD CONSTRAINT fk_sys_user_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id),
  ADD CONSTRAINT fk_sys_user_role FOREIGN KEY (role_id) REFERENCES sys_role (id);

CREATE INDEX idx_srp_permission ON sys_role_permission (permission_id);
CREATE INDEX idx_sys_user_role ON sys_user (role_id);
