INSERT INTO "user" (email, password_hash, role, is_active, created_at)
VALUES ('alice.seller@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'SELLER', true, CURRENT_TIMESTAMP),
       ('bob.seller@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'SELLER', true, CURRENT_TIMESTAMP),
       ('charlie.buyer@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'BUYER', true, CURRENT_TIMESTAMP),
       ('diana.admin@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'ADMIN', true, CURRENT_TIMESTAMP);

INSERT INTO seller_profile (user_id, store_name, store_description, contact_info, created_at)
VALUES (1, 'Alice Orchard', 'Fresh organic fruits and berries harvested on small local farms.', '+1-555-0101',
        CURRENT_TIMESTAMP),
       (2, 'Bob Greens', 'Seasonal vegetables and leafy greens delivered fresh every morning.', '+1-555-0102',
        CURRENT_TIMESTAMP);

INSERT INTO category (name, description, created_at)
VALUES ('Fresh Fruits', 'Fresh fruit selections from local growers.', CURRENT_TIMESTAMP),
       ('Vegetables', 'Fresh vegetables for daily meals and snacks.', CURRENT_TIMESTAMP),
       ('Citrus', 'Orange, lemon and grapefruit varieties.', CURRENT_TIMESTAMP),
       ('Leafy Greens', 'Spinach, lettuce and similar greens.', CURRENT_TIMESTAMP),
       ('Berries', 'Strawberries, blueberries and raspberries.', CURRENT_TIMESTAMP);

INSERT INTO product (seller_profile_id, title, description, price, stock_quantity, image_url, status, created_at)
VALUES (1, 'Organic Strawberry Pack', 'Sweet, ripe strawberries grown without synthetic pesticides.', 8.99, 40,
        'https://example.com/images/strawberries.jpg', 'ACTIVE', CURRENT_TIMESTAMP),
       (1, 'Green Apple Basket', 'Crisp and juicy apples picked at peak season.', 6.50, 50,
        'https://example.com/images/apples.jpg', 'ACTIVE', CURRENT_TIMESTAMP),
       (2, 'Baby Spinach Bunch', 'Tender baby spinach ideal for salads and smoothies.', 5.25, 60,
        'https://example.com/images/spinach.jpg', 'ACTIVE', CURRENT_TIMESTAMP),
       (2, 'Citrus Harvest Mix', 'A balanced mix of oranges and mandarins.', 9.99, 25,
        'https://example.com/images/citrus.jpg', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO product_category (product_id, category_id)
VALUES (1, 5),
       (1, 1),
       (2, 1),
       (3, 2),
       (3, 4),
       (4, 3);
