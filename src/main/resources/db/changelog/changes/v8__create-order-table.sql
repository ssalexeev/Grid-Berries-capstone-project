CREATE SEQUENCE IF NOT EXISTS order_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS "order" (
    id BIGINT PRIMARY KEY DEFAULT nextval('order_id_seq'),
    user_id bigint NOT NULL,
    status varchar(50) NOT NULL,
    total_amount decimal(10, 2) NOT NULL CHECK (total_amount >= 0),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_on_user
        FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_user_id ON "order"(user_id);
