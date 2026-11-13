CREATE SEQUENCE IF NOT EXISTS order_item_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS order_item
(
    id         BIGINT PRIMARY KEY      DEFAULT nextval('order_item_id_seq'),
    order_id   bigint         NOT NULL,
    product_id bigint         NOT NULL,
    quantity   int            NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price decimal(10, 2) NOT NULL CHECK (unit_price >= 0),
    created_at timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_item_on_order
        FOREIGN KEY (order_id) REFERENCES "order" (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_on_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item (order_id);
