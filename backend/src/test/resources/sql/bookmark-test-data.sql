INSERT INTO member (member_id, name, member_code, email, password) VALUES (1, 'Test User', 'testCode', 'test@test.com', 'test');

-- article_id를 직접 지정
INSERT INTO article (article_id, title, content, image_url, url)
VALUES (1, 'Test Article', 'Test Content', NULL, NULL);

INSERT INTO article (article_id, title, content, image_url, url)
VALUES (2, 'Test Article 2', 'Test Content 2', NULL, NULL);

-- tag
INSERT INTO tag (tag_id, name, article_id) VALUES (1, 'Java', 1);
INSERT INTO tag (tag_id, name, article_id) VALUES (2, 'Spring', 1);

-- bookmark
INSERT INTO bookmark (bookmark_id, member_id, article_id) VALUES (1, 1, 1);
INSERT INTO bookmark (bookmark_id, member_id, article_id) VALUES (2, 1, 2);