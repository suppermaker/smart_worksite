INSERT IGNORE INTO permission (permission_code, permission_name, permission_type, created_by, updated_by)
VALUES
  ('knowledge:manage', 'Knowledge Base Management', 'API', 1, 1),
  ('datasource:manage', 'Data Source Management', 'API', 1, 1);

INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 1, id, 1, 1 FROM permission
WHERE permission_code IN ('knowledge:manage', 'datasource:manage')
  AND deleted = 0;

INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 2, id, 1, 1 FROM permission
WHERE permission_code IN ('knowledge:manage', 'datasource:manage')
  AND deleted = 0;
