CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. USERS TABLE
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    bio TEXT,
    image TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- 触发器: users 表的任何行更新时，自动调用函数更新 updated_at
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

-- 2. ARTICLES TABLE
CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    body TEXT NOT NULL,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON articles
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

-- 3. COMMENTS TABLE
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    body TEXT NOT NULL,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON comments
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

-- 4. TAGS TABLE
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- 5. JUNCTION TABLES FOR MANY-TO-MANY RELATIONSHIPS

-- 用户关注关系表 (_UserFollows)
CREATE TABLE user_follows (
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followed_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (follower_id, followed_id)
);

-- 文章收藏关系表 (_ArticleFavorites)
CREATE TABLE article_favorites (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, article_id)
);

-- 文章标签关系表 (_ArticleTags)
CREATE TABLE article_tags (
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);


-- =================================================================
-- INDEXES FOR PERFORMANCE
-- =================================================================
CREATE INDEX ON articles (author_id);
CREATE INDEX ON comments (article_id);
CREATE INDEX ON comments (author_id);
CREATE INDEX ON user_follows (followed_id);
CREATE INDEX ON article_favorites (article_id);
CREATE INDEX ON article_tags (tag_id);