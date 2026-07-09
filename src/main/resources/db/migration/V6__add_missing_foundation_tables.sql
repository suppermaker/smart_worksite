CREATE TABLE IF NOT EXISTS qa_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
  project_id BIGINT NOT NULL COMMENT 'Project ID',
  title VARCHAR(255) NOT NULL COMMENT 'Session title',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Session status',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  created_by BIGINT NULL COMMENT 'Created by user ID',
  updated_by BIGINT NULL COMMENT 'Updated by user ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  KEY idx_qa_session_project (project_id),
  KEY idx_qa_session_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='QA session table';

CREATE TABLE IF NOT EXISTS qa_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
  project_id BIGINT NOT NULL COMMENT 'Project ID',
  session_id BIGINT NOT NULL COMMENT 'QA session ID',
  role VARCHAR(32) NOT NULL COMMENT 'Message role',
  question TEXT NULL COMMENT 'Question content',
  answer MEDIUMTEXT NULL COMMENT 'Answer content',
  route_mode VARCHAR(32) NULL COMMENT 'AI route mode',
  references_json JSON NULL COMMENT 'References and citations',
  feedback_json JSON NULL COMMENT 'Feedback data',
  status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT 'Message status',
  task_id BIGINT NULL COMMENT 'Task ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  created_by BIGINT NULL COMMENT 'Created by user ID',
  updated_by BIGINT NULL COMMENT 'Updated by user ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  KEY idx_qa_message_project (project_id),
  KEY idx_qa_message_session (session_id),
  KEY idx_qa_message_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='QA message table';

CREATE TABLE IF NOT EXISTS review_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
  project_id BIGINT NOT NULL COMMENT 'Project ID',
  template_id BIGINT NOT NULL COMMENT 'Template ID',
  file_id BIGINT NOT NULL COMMENT 'Reviewed file ID',
  task_id BIGINT NULL COMMENT 'Task ID',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Review status',
  issues_json JSON NULL COMMENT 'Issue list JSON',
  result_json JSON NULL COMMENT 'Review result JSON',
  error_message TEXT NULL COMMENT 'Error message',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  created_by BIGINT NULL COMMENT 'Created by user ID',
  updated_by BIGINT NULL COMMENT 'Updated by user ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  KEY idx_review_record_project (project_id),
  KEY idx_review_record_template (template_id),
  KEY idx_review_record_status (status),
  KEY idx_review_record_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Compliance review record table';

CREATE TABLE IF NOT EXISTS ocr_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
  project_id BIGINT NOT NULL COMMENT 'Project ID',
  ocr_type VARCHAR(64) NOT NULL COMMENT 'OCR type',
  file_id BIGINT NOT NULL COMMENT 'Source file ID',
  task_id BIGINT NULL COMMENT 'Task ID',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'OCR status',
  fields_json JSON NULL COMMENT 'Recognized fields JSON',
  custom_fields_json JSON NULL COMMENT 'Custom fields JSON',
  error_message TEXT NULL COMMENT 'Error message',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  created_by BIGINT NULL COMMENT 'Created by user ID',
  updated_by BIGINT NULL COMMENT 'Updated by user ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  KEY idx_ocr_record_project (project_id),
  KEY idx_ocr_record_type (ocr_type),
  KEY idx_ocr_record_status (status),
  KEY idx_ocr_record_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR record table';
