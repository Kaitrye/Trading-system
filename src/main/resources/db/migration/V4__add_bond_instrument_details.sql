ALTER TABLE instruments
    DROP CONSTRAINT ck_instruments_type;

ALTER TABLE instruments
    ADD COLUMN nominal NUMERIC(19, 4),
    ADD COLUMN maturity_date DATE,
    ADD COLUMN coupon_rate NUMERIC(7, 4),
    ADD COLUMN coupon_frequency_months INTEGER,
    ADD COLUMN next_coupon_date DATE;

ALTER TABLE instruments
    ADD CONSTRAINT ck_instruments_type CHECK (type IN ('STOCK', 'BOND')),
    ADD CONSTRAINT ck_instruments_bond_details CHECK (
        (type = 'STOCK'
            AND nominal IS NULL
            AND maturity_date IS NULL
            AND coupon_rate IS NULL
            AND coupon_frequency_months IS NULL
            AND next_coupon_date IS NULL)
        OR
        (type = 'BOND'
            AND nominal > 0
            AND maturity_date IS NOT NULL
            AND coupon_rate > 0
            AND coupon_frequency_months > 0
            AND next_coupon_date IS NOT NULL
            AND next_coupon_date <= maturity_date)
    );
