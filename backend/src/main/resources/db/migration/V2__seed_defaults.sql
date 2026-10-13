-- V2__seed_defaults.sql
-- Seed default garments (Shirt, Pant, Kurta, Blouse) with standard measurement fields in inches

-- 1. Shirt
INSERT INTO garment (id, name, sort_order, active)
VALUES (1, 'Shirt', 1, TRUE);

INSERT INTO garment_field (garment_id, label, unit, sort_order, active) VALUES
(1, 'Length', 'inch', 1, TRUE),
(1, 'Chest', 'inch', 2, TRUE),
(1, 'Waist', 'inch', 3, TRUE),
(1, 'Shoulder', 'inch', 4, TRUE),
(1, 'Sleeve Length', 'inch', 5, TRUE),
(1, 'Collar', 'inch', 6, TRUE);

-- 2. Pant
INSERT INTO garment (id, name, sort_order, active)
VALUES (2, 'Pant', 2, TRUE);

INSERT INTO garment_field (garment_id, label, unit, sort_order, active) VALUES
(2, 'Length', 'inch', 1, TRUE),
(2, 'Waist', 'inch', 2, TRUE),
(2, 'Hip', 'inch', 3, TRUE),
(2, 'Inseam', 'inch', 4, TRUE),
(2, 'Thigh', 'inch', 5, TRUE),
(2, 'Bottom', 'inch', 6, TRUE);

-- 3. Kurta
INSERT INTO garment (id, name, sort_order, active)
VALUES (3, 'Kurta', 3, TRUE);

INSERT INTO garment_field (garment_id, label, unit, sort_order, active) VALUES
(3, 'Length', 'inch', 1, TRUE),
(3, 'Chest', 'inch', 2, TRUE),
(3, 'Waist', 'inch', 3, TRUE),
(3, 'Shoulder', 'inch', 4, TRUE),
(3, 'Sleeve Length', 'inch', 5, TRUE),
(3, 'Neck', 'inch', 6, TRUE);

-- 4. Blouse
INSERT INTO garment (id, name, sort_order, active)
VALUES (4, 'Blouse', 4, TRUE);

INSERT INTO garment_field (garment_id, label, unit, sort_order, active) VALUES
(4, 'Length', 'inch', 1, TRUE),
(4, 'Bust', 'inch', 2, TRUE),
(4, 'Under Bust / Waist', 'inch', 3, TRUE),
(4, 'Shoulder', 'inch', 4, TRUE),
(4, 'Front Neck', 'inch', 5, TRUE),
(4, 'Back Neck', 'inch', 6, TRUE),
(4, 'Sleeve Length', 'inch', 7, TRUE);

-- Sync identity sequences with inserted primary keys so future auto-generated IDs do not collide
SELECT setval(pg_get_serial_sequence('garment', 'id'), (SELECT MAX(id) FROM garment));
SELECT setval(pg_get_serial_sequence('garment_field', 'id'), (SELECT MAX(id) FROM garment_field));
