CREATE SEQUENCE IF NOT EXISTS cart_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT PRIMARY KEY DEFAULT nextval('cart_id_seq'),
    user_id bigint NOT NULL UNIQUE,
    total_amount decimal(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_on_user
        FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
