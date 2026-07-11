INSERT IGNORE INTO permission (permission_code, permission_name, permission_type, created_by, updated_by)
VALUES
  ('qa:manage', 'QA Management', 'API', 1, 1);

INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 1, id, 1, 1 FROM permission
WHERE permission_code = 'qa:manage'
  AND deleted = 0;

INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 2, id, 1, 1 FROM permission
WHERE permission_code = 'qa:manage'
  AND deleted = 0;

ALTER TABLE qa_session
  ADD KEY idx_qa_session_status (status),
  ADD KEY idx_qa_session_updated_at (updated_at);

ALTER TABLE qa_message
  ADD KEY idx_qa_message_status (status),
  ADD KEY idx_qa_message_created_at (created_at);
