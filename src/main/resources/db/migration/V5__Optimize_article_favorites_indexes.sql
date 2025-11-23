DROP INDEX IF EXISTS idx_article_favorites_user_id;


CREATE INDEX idx_article_favorites_article_id ON article_favorites (article_id);