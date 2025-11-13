CREATE DATABASE IF NOT EXISTS smart_inventory;
USE smart_inventory;

-- Products table
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100),
    price DECIMAL(10,2),
    quantity INT
);

-- Customers table
CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100),
    phone VARCHAR(15)
);

-- Bills table
CREATE TABLE bills (
    bill_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    total DECIMAL(10,2),
    bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Bill items
CREATE TABLE bill_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id INT,
    product_id INT,
    quantity INT,
    subtotal DECIMAL(10,2),
    FOREIGN KEY (bill_id) REFERENCES bills(bill_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Sample data
INSERT INTO products (product_name, price, quantity) VALUES
('Milk Packet', 25.00, 100),
('Bread', 20.00, 50),
('Sugar (1kg)', 45.00, 60),
('Rice (5kg)', 250.00, 30),
('Tea Powder (500g)', 80.00, 40);
