ALTER TABLE redis_cleanup_tasks
    RENAME TO redis_tasks;

ALTER TABLE redis_tasks
    ADD COLUMN operation VARCHAR(20);

UPDATE redis_tasks
SET operation = 'RELEASE';

ALTER TABLE redis_tasks
    ALTER COLUMN operation SET NOT NULL;

ALTER TABLE redis_tasks
    ADD COLUMN id UUID;

UPDATE redis_tasks
SET id = gen_random_uuid();

ALTER TABLE redis_tasks
    DROP CONSTRAINT redis_cleanup_tasks_pkey;

ALTER TABLE redis_tasks
    ADD PRIMARY KEY (id);

ALTER TABLE redis_tasks
    ALTER COLUMN booking_id SET NOT NULL;

ALTER TABLE redis_tasks
    ADD CONSTRAINT uk_redis_tasks_booking_operation
        UNIQUE (booking_id, operation);