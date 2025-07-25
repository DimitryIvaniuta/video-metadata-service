-- Inserts two hardcoded users: admin / user
-- Passwords are BCrypt hashes of "adminpass" and "userpass" respectively.

-- 1) Insert into users admin, get back the generated id
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
    'admin',
    '$2a$10$aoxiGHlH0yJDqbT2C8DLW.RN2V0VJNxkhKwLd9zvd5c9WZcCRaX52',
    'admin@example.com',
    0,
    now(),
    now()
    )
    RETURNING id
    )
-- 2) Grant the ADMIN role to that user
INSERT INTO user_roles (
    id,
    user_id,
    role
)
SELECT
    nextval('VM_UNIQUE_ID'),
    id,
    'ADMIN'
FROM new_user;

-- 2) Insert into users user, get back the generated id
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
    'user',
    '$2a$10$.lOsTK14qr.2SB0S7.Wt.uB0V22KvNhtwtzrptuPq8xcF0WOSd5bq',
    'user@example.com',
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
