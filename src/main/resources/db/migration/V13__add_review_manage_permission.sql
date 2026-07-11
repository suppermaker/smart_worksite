INSERT IGNORE INTO permission (permission_code, permission_name, permission_type, created_by, updated_by)
VALUES
  ('review:manage', 'Compliance Review Management', 'API', 1, 1);

INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 1, id, 1, 1 FROM permission
WHERE permission_code = 'review:manage'
  AND deleted = 0;

INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 2, id, 1, 1 FROM permission
WHERE permission_code = 'review:manage'
  AND deleted = 0;

ALTER TABLE review_record
  ADD KEY idx_review_record_file (file_id),
  ADD KEY idx_review_record_created_at (created_at);
