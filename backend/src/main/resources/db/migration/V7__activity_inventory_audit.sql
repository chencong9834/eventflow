CREATE TABLE activity (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description TEXT NULL,
  cover_url VARCHAR(512) NULL,
  review_status VARCHAR(32) NOT NULL,
  sale_status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT chk_activity_review CHECK (review_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
  CONSTRAINT chk_activity_sale CHECK (sale_status IN ('CLOSED', 'ON_SALE')),
  CONSTRAINT fk_activity_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_activity_tenant ON activity (tenant_id, review_status);
CREATE INDEX idx_activity_review ON activity (review_status, updated_at);

CREATE TABLE activity_show (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  start_at TIMESTAMP(3) NOT NULL,
  end_at TIMESTAMP(3) NOT NULL,
  sale_start_at TIMESTAMP(3) NOT NULL,
  sale_end_at TIMESTAMP(3) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT fk_show_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
  CONSTRAINT fk_show_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_show_activity ON activity_show (activity_id);

CREATE TABLE ticket_tier (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  show_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  unit_price_fen BIGINT NOT NULL,
  per_user_limit INT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT chk_tier_price CHECK (unit_price_fen >= 0),
  CONSTRAINT chk_tier_limit CHECK (per_user_limit >= 1),
  CONSTRAINT fk_tier_show FOREIGN KEY (show_id) REFERENCES activity_show (id),
  CONSTRAINT fk_tier_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tier_show ON ticket_tier (show_id);

CREATE TABLE inventory (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  ticket_tier_id BIGINT NOT NULL,
  total_qty INT NOT NULL,
  available_qty INT NOT NULL,
  reserved_qty INT NOT NULL,
  sold_qty INT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_inventory_tier UNIQUE (ticket_tier_id),
  CONSTRAINT chk_inventory_qty CHECK (
    total_qty >= 0
    AND available_qty >= 0
    AND reserved_qty >= 0
    AND sold_qty >= 0
    AND available_qty + reserved_qty + sold_qty = total_qty
  ),
  CONSTRAINT fk_inventory_tier FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tier (id),
  CONSTRAINT fk_inventory_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_operation_log (
  id BIGINT NOT NULL PRIMARY KEY,
  actor_tenant_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  object_type VARCHAR(32) NOT NULL,
  object_id BIGINT NOT NULL,
  object_tenant_id BIGINT NOT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NOT NULL,
  comment VARCHAR(512) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_audit_actor_tenant FOREIGN KEY (actor_tenant_id) REFERENCES sys_tenant (id),
  CONSTRAINT fk_audit_object_tenant FOREIGN KEY (object_tenant_id) REFERENCES sys_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_object ON audit_operation_log (object_type, object_id, created_at);
