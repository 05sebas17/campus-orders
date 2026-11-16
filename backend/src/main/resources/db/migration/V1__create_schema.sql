CREATE TABLE products (
 id uuid PRIMARY KEY, sku varchar(50) NOT NULL UNIQUE, name varchar(150) NOT NULL,
 description varchar(500), price numeric(14,2) NOT NULL CHECK(price>=0), active boolean NOT NULL
);
CREATE TABLE orders (
 id uuid PRIMARY KEY, number varchar(40) NOT NULL UNIQUE, customer_name varchar(120) NOT NULL,
 email varchar(254) NOT NULL, phone varchar(30), street varchar(200) NOT NULL, city varchar(100) NOT NULL,
 region varchar(100) NOT NULL, postal_code varchar(20), country varchar(2) NOT NULL,
 status varchar(20) NOT NULL CHECK(status IN ('PENDING','IN_PROGRESS','SHIPPED','DELIVERED')),
 currency varchar(3) NOT NULL CHECK(currency='COP'), total numeric(18,2) NOT NULL CHECK(total>=0),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0
);
CREATE TABLE order_items (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES orders(id), product_id uuid NOT NULL REFERENCES products(id),
 sku varchar(50) NOT NULL, name varchar(150) NOT NULL, unit_price numeric(14,2) NOT NULL CHECK(unit_price>=0),
 quantity integer NOT NULL CHECK(quantity BETWEEN 1 AND 1000), subtotal numeric(18,2) NOT NULL CHECK(subtotal=unit_price*quantity),
 UNIQUE(order_id,product_id)
);
CREATE TABLE order_status_history (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES orders(id), previous_status varchar(20), new_status varchar(20) NOT NULL,
 changed_at timestamptz NOT NULL,
 CHECK ((previous_status IS NULL AND new_status='PENDING') OR (previous_status='PENDING' AND new_status='IN_PROGRESS') OR (previous_status='IN_PROGRESS' AND new_status='SHIPPED') OR (previous_status='SHIPPED' AND new_status='DELIVERED')),
 UNIQUE(order_id,new_status)
);
CREATE INDEX orders_created_idx ON orders(created_at,id);
CREATE INDEX orders_status_created_idx ON orders(status,created_at,id);
CREATE INDEX orders_total_idx ON orders(total,id);
CREATE INDEX history_order_idx ON order_status_history(order_id,changed_at);
