-- Add business-level permissions used by the frontend router.
INSERT IGNORE INTO permission (permission_code, permission_name, permission_type, created_by, updated_by)
VALUES
  ('dashboard:view',        'Dashboard View',              'API', 1, 1),
  ('knowledge:view',        'Knowledge Base View',         'API', 1, 1),
  ('qa:view',               'QA View',                     'API', 1, 1),
  ('review:view',           'Compliance Review View',      'API', 1, 1),
  ('report:view',           'Report View',                 'API', 1, 1),
  ('ocr:view',              'OCR View',                    'API', 1, 1),
  ('system:user:manage',    'System User Management',      'API', 1, 1),
  ('project:member:manage', 'Project Member Management',   'API', 1, 1);

-- PLATFORM_ADMIN: all permissions.
INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 1, id, 1, 1 FROM permission
WHERE permission_code IN (
  'dashboard:view','knowledge:view','qa:view','review:view','report:view','ocr:view',
  'system:user:manage','project:member:manage','system:manage','project:view','project:manage','file:manage'
) AND deleted = 0;

-- PROJECT_ADMIN: business + member management.
INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 2, id, 1, 1 FROM permission
WHERE permission_code IN (
  'dashboard:view','knowledge:view','qa:view','review:view','report:view','ocr:view',
  'project:member:manage','project:view','file:manage'
) AND deleted = 0;

-- BUSINESS_USER: full business read/write, no admin.
INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 3, id, 1, 1 FROM permission
WHERE permission_code IN (
  'dashboard:view','knowledge:view','qa:view','review:view','report:view','ocr:view',
  'project:view','file:manage'
) AND deleted = 0;

-- VIEWER: read-only.
INSERT IGNORE INTO role_permission (role_id, permission_id, created_by, updated_by)
SELECT 4, id, 1, 1 FROM permission
WHERE permission_code IN (
  'dashboard:view','knowledge:view','qa:view','review:view','report:view','project:view'
) AND deleted = 0;
