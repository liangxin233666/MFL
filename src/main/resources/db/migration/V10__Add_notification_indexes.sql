CREATE INDEX IF NOT EXISTS idx_notifications_user_read
ON notifications (target_user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_notifications_user_time
ON notifications (target_user_id, created_at DESC);