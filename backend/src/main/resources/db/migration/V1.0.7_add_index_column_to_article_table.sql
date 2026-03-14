ALTER TABLE article
    ADD UNIQUE KEY uk_article_url (url),
    ADD KEY idx_article_created_at (created_at),
    ADD KEY idx_article_status_created (status, created_at);