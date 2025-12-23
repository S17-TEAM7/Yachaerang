-- reactions table 생성
CREATE TABLE IF NOT EXISTS reactions (
    reaction_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (reaction_id),
    UNIQUE KEY uk_reactions_member_article (member_id, article_id),
    KEY idx_reactions_article_member (member_id, article_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
