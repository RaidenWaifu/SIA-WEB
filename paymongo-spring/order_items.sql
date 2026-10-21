-- Run this in phpMyAdmin (or MySQL Workbench / mysql CLI) against your
-- existing paymongo_shop database. Safe to run even if orders already exists —
-- it only creates order_items if it's missing.
--
-- Assumes your existing `orders` table looks like:
--   id (PK), order_code (unique varchar), status (enum/varchar), 
--   total_centavos (int), checkout_session_id (varchar, nullable), created_at (timestamp)

USE paymongo_shop;

CREATE TABLE IF NOT EXISTS order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT DEFAULT NULL,
    name VARCHAR(150) NOT NULL,
    price_centavos INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
