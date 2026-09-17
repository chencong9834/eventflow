CREATE TABLE ticket_order (
  id BIGINT NOT NULL PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  tenant_id BIGINT NOT NULL,
  buyer_user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  show_id BIGINT NOT NULL,
  ticket_tier_id BIGINT NOT NULL,
  activity_title VARCHAR(128) NOT NULL,
  show_name VARCHAR(128) NOT NULL,
  tier_name VARCHAR(64) NOT NULL,
  qty INT NOT NULL,
  unit_price_fen BIGINT NOT NULL,
  amount_fen BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  pay_deadline_at TIMESTAMP(3) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT NULL,
  CONSTRAINT uk_ticket_order_no UNIQUE (order_no),
  CONSTRAINT chk_ticket_order_qty CHECK (qty >= 1),
  CONSTRAINT chk_ticket_order_amount CHECK (amount_fen >= 0 AND unit_price_fen >= 0),
  CONSTRAINT chk_ticket_order_status CHECK (status IN ('CREATED', 'PAID', 'CANCELLED', 'REFUNDED', 'FULFILLED')),
  CONSTRAINT fk_order_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id),
  CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_order_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
  CONSTRAINT fk_order_show FOREIGN KEY (show_id) REFERENCES activity_show (id),
  CONSTRAINT fk_order_tier FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tier (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_order_buyer ON ticket_order (buyer_user_id, created_at);
CREATE INDEX idx_order_tenant ON ticket_order (tenant_id, created_at);
CREATE INDEX idx_order_status_deadline ON ticket_order (status, pay_deadline_at);

CREATE TABLE payment (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  amount_fen BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  simulated_result VARCHAR(32) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_payment_order UNIQUE (order_id),
  CONSTRAINT chk_payment_channel CHECK (channel = 'SIMULATED'),
  CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES ticket_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ticket (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  buyer_user_id BIGINT NOT NULL,
  show_id BIGINT NOT NULL,
  ticket_tier_id BIGINT NOT NULL,
  ticket_no VARCHAR(64) NOT NULL,
  verify_code VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  used_at TIMESTAMP(3) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_ticket_no UNIQUE (ticket_no),
  CONSTRAINT uk_ticket_verify UNIQUE (verify_code),
  CONSTRAINT chk_ticket_status CHECK (status IN ('UNUSED', 'USED', 'VOID')),
  CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES ticket_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ticket_order ON ticket (order_id);
CREATE INDEX idx_ticket_buyer ON ticket (buyer_user_id);
CREATE INDEX idx_ticket_tenant ON ticket (tenant_id);

CREATE TABLE ticket_refund (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  amount_fen BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_refund_order UNIQUE (order_id),
  CONSTRAINT chk_refund_status CHECK (status IN ('REQUESTED', 'SUCCEEDED', 'REJECTED')),
  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES ticket_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_event (
  id BIGINT NOT NULL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  published TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  published_at TIMESTAMP(3) NULL,
  CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_outbox_unpublished ON outbox_event (published, id);

CREATE TABLE inbox_event (
  event_id VARCHAR(64) NOT NULL PRIMARY KEY,
  event_type VARCHAR(64) NOT NULL,
  processed_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site_notice (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  body VARCHAR(512) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_notice_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notice_user ON site_notice (user_id, created_at);
