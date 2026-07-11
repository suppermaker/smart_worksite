ALTER TABLE generate_task
  ADD COLUMN cancel_requested TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether cancel has been requested' AFTER error_message,
  ADD COLUMN worker_id VARCHAR(128) NULL COMMENT 'Claimed worker ID' AFTER cancel_requested,
  ADD COLUMN lease_until DATETIME NULL COMMENT 'Worker lease expiration time' AFTER worker_id,
  ADD COLUMN last_heartbeat_at DATETIME NULL COMMENT 'Last worker heartbeat time' AFTER lease_until;

ALTER TABLE generate_task
  ADD KEY idx_task_lease (status, lease_until),
  ADD KEY idx_task_cancel_requested (cancel_requested);

ALTER TABLE task_stage_log
  ADD COLUMN attempt_no INT NOT NULL DEFAULT 1 COMMENT 'Task attempt number' AFTER task_id;

CREATE TABLE task_outbox (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
  task_id BIGINT NOT NULL COMMENT 'Task ID',
  project_id BIGINT NOT NULL COMMENT 'Project ID',
  event_type VARCHAR(64) NOT NULL COMMENT 'Task event type',
  payload JSON NULL COMMENT 'Task event payload',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Outbox status',
  delivery_count INT NOT NULL DEFAULT 0 COMMENT 'Delivery count',
  next_delivery_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Next delivery time',
  error_message TEXT NULL COMMENT 'Last delivery error',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  created_by BIGINT NULL COMMENT 'Created by user ID',
  updated_by BIGINT NULL COMMENT 'Updated by user ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  UNIQUE KEY uk_task_outbox_event (task_id, event_type, deleted),
  KEY idx_task_outbox_status (status, next_delivery_at),
  KEY idx_task_outbox_project (project_id),
  KEY idx_task_outbox_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task reliable delivery outbox';
