CREATE SEQUENCE IF NOT EXISTS cart_item_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGINT PRIMARY KEY DEFAULT nextval('cart_item_id_seq'),
    cart_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity int NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_item_on_cart
        FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_on_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cart_item_cart_id ON cart_item(cart_id);
