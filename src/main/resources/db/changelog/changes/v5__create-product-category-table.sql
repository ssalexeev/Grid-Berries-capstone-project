CREATE TABLE IF NOT EXISTS product_category (
    product_id bigint NOT NULL,
    category_id bigint NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_category_on_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_category_on_category
        FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_category_category_id ON product_category(category_id);
