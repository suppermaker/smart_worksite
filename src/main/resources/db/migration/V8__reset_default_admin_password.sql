-- Reset local default admin password to match documented development credentials.
UPDATE user_account
SET password_hash = '$2b$10$9hLhRwof2iw2.BfewxfqIeiBylUFnG2Fb4gegI321BMt1sJRtjHV.',
    updated_by = 1
WHERE username = 'admin'
  AND deleted = 0;
