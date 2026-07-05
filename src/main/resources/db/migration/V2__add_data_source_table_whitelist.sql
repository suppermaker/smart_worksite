ALTER TABLE data_source
  ADD COLUMN table_whitelist_json TEXT NULL COMMENT 'JSON array of allowed table names for read-only database question validation'
  AFTER status;
