ALTER TABLE knowledge_document
  ADD COLUMN task_id BIGINT NULL COMMENT 'Latest knowledge indexing task ID' AFTER index_status,
  ADD COLUMN error_message TEXT NULL COMMENT 'Latest knowledge indexing error' AFTER task_id,
  ADD KEY idx_kdoc_task (task_id);
