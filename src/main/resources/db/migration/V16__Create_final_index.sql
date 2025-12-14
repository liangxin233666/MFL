-- 1. 社交Feed流的核心索引 (Followed Authors -> Status -> Latest)
CREATE INDEX IF NOT EXISTS idx_articles_feed_perf
ON articles (author_id, status, created_at DESC);

-- 2. 全网热榜计算核心索引 (Status -> Favorites Desc)
CREATE INDEX IF NOT EXISTS idx_articles_hot_perf
ON articles (status, favorites_count DESC);

-- 3. 全站最新文章列表
CREATE INDEX IF NOT EXISTS idx_articles_latest_public
ON articles (status, created_at DESC);

