ALTER TABLE instruments ADD COLUMN name VARCHAR(120);

UPDATE instruments
SET name = seccode
WHERE name IS NULL;

ALTER TABLE instruments ALTER COLUMN name SET NOT NULL;
ALTER TABLE instruments ADD CONSTRAINT ck_instruments_name_not_blank CHECK (btrim(name) <> '');
