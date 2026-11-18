ALTER TABLE accounts
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'CLOSED'));
