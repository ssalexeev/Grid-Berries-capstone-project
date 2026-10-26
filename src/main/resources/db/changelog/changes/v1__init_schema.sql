CREATE TABLE IF NOT EXISTS "user" (
    id serial PRIMARY KEY,
    email varchar(100) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role varchar(50) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS seller_profile (
    id serial PRIMARY KEY,
    user_id int NOT NULL,
    store_name varchar(100) NOT NULL,
    store_description text,
    contact_info text,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_seller_profile_on_user
        FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product (
    id serial PRIMARY KEY,
    seller_id int,
    title varchar(100) NOT NULL,
    description text,
    price decimal(10, 2) NOT NULL,
    stock_quantity int DEFAULT 0,
    image_url varchar(255),
    type varchar(50) NOT NULL,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_product_on_seller_profile
        FOREIGN KEY (seller_id) REFERENCES seller_profile(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS category (
    id serial PRIMARY KEY,
    parent_category_id int,
    name varchar(100) NOT NULL UNIQUE,
    description text,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_category_on_category
        FOREIGN KEY (parent_category_id) REFERENCES category(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS product_category (
    product_id int NOT NULL,
    category_id int NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_category_on_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_category_on_category
        FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart (
    id serial PRIMARY KEY,
    user_id int,
    total_amount decimal(10, 2) DEFAULT 0.00,
    updated_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_cart_on_user
        FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "order" (
    id serial PRIMARY KEY,
    user_id int,
    status varchar(50),
    total_amount decimal(10, 2) NOT NULL,
    created_at timestamp DEFAULT NOW(),
    updated_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_order_on_user
        FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart_item (
    id serial PRIMARY KEY,
    cart_id int,
    product_id int,
    quantity int DEFAULT 1,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_cart_item_on_cart
        FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_on_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_item (
    id serial PRIMARY KEY,
    order_id int,
    product_id int,
    quantity int DEFAULT 1,
    unit_price decimal(10, 2) NOT NULL,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_order_item_on_order
        FOREIGN KEY (order_id) REFERENCES "order"(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_on_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE  IF NOT EXISTS sales_analytics_report (
    id serial PRIMARY KEY,
    product_id int,
    period_start date NOT NULL,
    period_end date NOT NULL,
    aggregated_metrics jsonb,
    ai_insights_text text,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_sales_analytics_report_on_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_metric_event (
    id serial PRIMARY KEY,
    user_id int,
    event_type varchar(100) NOT NULL,
    search_query text,
    created_at timestamp DEFAULT NOW(),
    CONSTRAINT fk_user_metric_event_on_user
        FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
