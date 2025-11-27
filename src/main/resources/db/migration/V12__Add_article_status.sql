-- 给 articles 表添加 status 字段，默认值为 'PENDING'
ALTER TABLE articles
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';

-- (可选) 如果你希望以前发布的文章都视为“已发布”，可以运行这句：
UPDATE articles SET status = 'PUBLISHED';