ALTER TABLE comments ADD COLUMN root_id BIGINT;

WITH RECURSIVE comment_paths (id, root_id) AS (

    SELECT id, id AS root_id FROM comments WHERE parent_id IS NULL
    UNION ALL
    SELECT c.id, p.root_id FROM comments c
    JOIN comment_paths p ON c.parent_id = p.id
)

UPDATE comments
SET root_id = cp.root_id
FROM comment_paths cp
WHERE comments.id = cp.id;

ALTER TABLE comments DROP CONSTRAINT fk_comments_parent_id;

ALTER TABLE comments
ADD CONSTRAINT fk_comments_parent_id
FOREIGN KEY (parent_id)
REFERENCES comments(id)
ON DELETE SET NULL;

ALTER TABLE comments
ADD CONSTRAINT fk_comments_root_id
FOREIGN KEY (root_id)
REFERENCES comments(id)
ON DELETE CASCADE;

CREATE INDEX idx_comments_parent_id ON comments (parent_id);

CREATE INDEX idx_comments_root_id_created_at ON comments (root_id, created_at);
