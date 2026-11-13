CREATE SEQUENCE IF NOT EXISTS seller_profile_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS seller_profile
(
    id                BIGINT PRIMARY KEY    DEFAULT nextval('seller_profile_id_seq'),
    user_id           bigint       NOT NULL UNIQUE,
    store_name        varchar(100) NOT NULL,
    store_description text,
    contact_info      varchar(100),
    created_at        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_profile_on_user
        FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);
