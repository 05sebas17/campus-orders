-- Solo perfil local. Repetible al cambiar el checksum; no sobrescribe pedidos existentes.
INSERT INTO products(id,sku,name,description,price,active) VALUES
('11111111-1111-4111-8111-111111111111','UNI-001','Cuaderno universitario','100 hojas, papel de 90 g',18500,true),
('22222222-2222-4222-8222-222222222222','UNI-002','Kit de escritura','Tres bolÃ­grafos y un resaltador',12900,true),
('33333333-3333-4333-8333-333333333333','UNI-003','Termo de acero','Botella reutilizable de 600 ml',45900,true),
('44444444-4444-4444-8444-444444444444','UNI-004','Mochila campus','Compartimento para portÃ¡til',129900,true) ON CONFLICT (id) DO NOTHING;
INSERT INTO orders(id,number,customer_name,email,street,city,region,postal_code,country,status,currency,total,created_at,updated_at,version)
VALUES ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','PED-DEMO-001','Cliente de demostraciÃ³n','demo@example.com','Calle de ejemplo 123','BogotÃ¡','BogotÃ¡ D.C.','110111','CO','PENDING','COP',37000,now(),now(),0) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items VALUES('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','11111111-1111-4111-8111-111111111111','UNI-001','Cuaderno universitario',18500,2,37000) ON CONFLICT (id) DO NOTHING;
INSERT INTO order_status_history VALUES('cccccccc-cccc-4ccc-8ccc-cccccccccccc','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',NULL,'PENDING',now()) ON CONFLICT (id) DO NOTHING;
