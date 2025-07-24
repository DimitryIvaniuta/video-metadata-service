-- Users table
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       NOT NULL PRIMARY KEY DEFAULT nextval('VM_UNIQUE_ID'),
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password       VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    status         SMALLINT     NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ
    );

-- User roles table (many‐to‐many via join table)
CREATE TABLE IF NOT EXISTS user_roles (
    id       BIGINT      NOT NULL PRIMARY KEY DEFAULT nextval('VM_UNIQUE_ID'),
    user_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role     VARCHAR(20) NOT NULL,
    UNIQUE(user_id, role)
);
