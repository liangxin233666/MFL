ALTER TABLE comments
ADD COLUMN parent_id BIGINT;


ALTER TABLE comments
ADD CONSTRAINT fk_comments_parent_id
FOREIGN KEY (parent_id)
REFERENCES comments(id)
ON DELETE CASCADE;