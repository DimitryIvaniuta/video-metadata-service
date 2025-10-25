WITH new_user AS (
INSERT INTO users (
    id,
    username,
    password,
    email,
    status,
    created_at,
    updated_at
)
VALUES (
    nextval('VM_UNIQUE_ID'),
    'user2',
    '$2a$10$.lOsTK14qr.2SB0S7.Wt.uB0V22KvNhtwtzrptuPq8xcF0WOSd5bq',
    'user2@example.com',
    0,
    now(),
    now()
    )
    RETURNING id
    )
-- 2) Grant the USER role to that user
INSERT INTO user_roles (
    id,
    user_id,
    role
)
SELECT
    nextval('VM_UNIQUE_ID'),
    id,
    'USER'
FROM new_user;

WITH new_user AS (
INSERT INTO users (
    id,
    username,
    password,
    email,
    status,
    created_at,
    updated_at
)
VALUES (
    nextval('VM_UNIQUE_ID'),
    'user3',
    '$2a$10$.lOsTK14qr.2SB0S7.Wt.uB0V22KvNhtwtzrptuPq8xcF0WOSd5bq',
    'user3@example.com',
    0,
    now(),
    now()
    )
    RETURNING id
    )
-- 2) Grant the USER role to that user
INSERT INTO user_roles (
    id,
    user_id,
    role
)
SELECT
    nextval('VM_UNIQUE_ID'),
    id,
    'USER'
FROM new_user;

WITH new_user AS (
INSERT INTO users (
    id,
    username,
    password,
    email,
    status,
    created_at,
    updated_at
)
VALUES (
    nextval('VM_UNIQUE_ID'),
    'user4',
    '$2a$10$.lOsTK14qr.2SB0S7.Wt.uB0V22KvNhtwtzrptuPq8xcF0WOSd5bq',
    'user4@example.com',
    0,
    now(),
    now()
    )
    RETURNING id
    )
-- 2) Grant the USER role to that user
INSERT INTO user_roles (
    id,
    user_id,
    role
)
SELECT
    nextval('VM_UNIQUE_ID'),
    id,
    'USER'
FROM new_user;

WITH new_user AS (
INSERT INTO users (
    id,
    username,
    password,
    email,
    status,
    created_at,
    updated_at
)
VALUES (
    nextval('VM_UNIQUE_ID'),
    'user5',
    '$2a$10$.lOsTK14qr.2SB0S7.Wt.uB0V22KvNhtwtzrptuPq8xcF0WOSd5bq',
    'user5@example.com',
    0,
    now(),
    now()
    )
    RETURNING id
    )
-- 2) Grant the USER role to that user
INSERT INTO user_roles (
    id,
    user_id,
    role
)
SELECT
    nextval('VM_UNIQUE_ID'),
    id,
    'USER'
FROM new_user;