ALTER TABLE article
    ADD COLUMN content_hash CHAR(64) NULL AFTER content,
  ADD COLUMN image_map JSON NULL AFTER cover_image_url,
  ADD COLUMN image_count INT NOT NULL DEFAULT 0 AFTER image_map,
  ADD COLUMN source_host VARCHAR(255) NULL AFTER url,
  ADD COLUMN fetched_at TIMESTAMP NULL AFTER source_host,
  ADD COLUMN status ENUM('pending','success','failed') NOT NULL DEFAULT 'pending' AFTER fetched_at,
  ADD COLUMN error_message TEXT NULL AFTER status;