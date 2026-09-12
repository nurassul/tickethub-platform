CREATE TABLE processed_events
(
    consumer_name VARCHAR(100) NOT NULL,
    event_id      UUID         NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CONSTRAINT pk_processed_events PRIMARY KEY (consumer_name, event_id)

);