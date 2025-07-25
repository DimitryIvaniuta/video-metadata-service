CREATE TABLE IF NOT EXISTS videos (
    id                BIGINT      NOT NULL PRIMARY KEY DEFAULT nextval('VM_UNIQUE_ID'),
    title             VARCHAR(255) NOT NULL,
    source            VARCHAR(100) NOT NULL,
    duration_ms       BIGINT      NOT NULL,
    description       TEXT        NOT NULL,
    category          SMALLINT    NOT NULL DEFAULT 0,
    provider          SMALLINT    NOT NULL DEFAULT 0,
    external_video_id VARCHAR(255),
    upload_date       TIMESTAMPTZ NOT NULL,
    created_user_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE
);