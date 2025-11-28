-- 实际上 text 或者 jsonb 存数组在 Java 里处理很方便
-- 我们这里用 text，里面存逗号分隔的字符串 "0.1,0.2..."，兼容性最好
ALTER TABLE users ADD COLUMN embedding_vector TEXT;