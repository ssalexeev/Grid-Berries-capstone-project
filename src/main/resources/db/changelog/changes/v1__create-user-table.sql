CREATE SEQUENCE IF NOT EXISTS user_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS "user"
(
    id            BIGINT PRIMARY KEY    DEFAULT nextval('user_id_seq'),
    email         varchar(100) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          varchar(50)  NOT NULL,
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
