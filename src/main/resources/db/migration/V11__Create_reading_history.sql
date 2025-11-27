CREATE TABLE reading_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    viewed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
);

-- 为了性能，必须加的复合唯一索引
-- 1. 它可以快速找到“这个用户有没有读过这篇文章？”
-- 2. 它可以作为数据库层面的去重约束（如果不希望有重复记录，或者采用"update time"策略）
-- 不过为了性能最佳，通常我们的策略是："Insert On Conflict Update" (Upsert)
CREATE UNIQUE INDEX idx_reading_history_user_article ON reading_history (user_id, article_id);

-- 为了列表查询的排序性能
CREATE INDEX idx_reading_history_user_viewed ON reading_history (user_id, viewed_at DESC);