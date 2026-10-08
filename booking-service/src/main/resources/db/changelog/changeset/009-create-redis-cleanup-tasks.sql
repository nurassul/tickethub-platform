CREATE TABLE redis_cleanup_tasks
(
    booking_id UUID PRIMARY KEY ,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0
);


CREATE INDEX idx_redis_cleanup_tasks
    ON redis_cleanup_tasks (next_attempt_at);
