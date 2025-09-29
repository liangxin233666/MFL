
CREATE UNIQUE INDEX idx_articles_slug ON articles (slug);


CREATE INDEX idx_comments_article_id_parent_id ON comments (article_id, parent_id);

CREATE INDEX idx_user_follows_follower_id ON user_follows (follower_id);


CREATE INDEX idx_article_favorites_user_id ON article_favorites (user_id);
