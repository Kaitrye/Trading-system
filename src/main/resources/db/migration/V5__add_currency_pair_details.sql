ALTER TABLE instruments
    DROP CONSTRAINT ck_instruments_type,
    DROP CONSTRAINT ck_instruments_bond_details;

ALTER TABLE instruments
    ADD COLUMN base_currency CHAR(3),
    ADD COLUMN quote_currency CHAR(3);

ALTER TABLE instruments
    ADD CONSTRAINT ck_instruments_type CHECK (type IN ('STOCK', 'BOND', 'CURRENCY_PAIR')),
    ADD CONSTRAINT ck_instruments_details CHECK (
        (type = 'STOCK'
            AND nominal IS NULL
            AND maturity_date IS NULL
            AND coupon_rate IS NULL
            AND coupon_frequency_months IS NULL
            AND next_coupon_date IS NULL
            AND base_currency IS NULL
            AND quote_currency IS NULL)
        OR
        (type = 'BOND'
            AND nominal > 0
            AND maturity_date IS NOT NULL
            AND coupon_rate > 0
            AND coupon_frequency_months > 0
            AND next_coupon_date IS NOT NULL
            AND next_coupon_date <= maturity_date
            AND base_currency IS NULL
            AND quote_currency IS NULL)
        OR
        (type = 'CURRENCY_PAIR'
            AND nominal IS NULL
            AND maturity_date IS NULL
            AND coupon_rate IS NULL
            AND coupon_frequency_months IS NULL
            AND next_coupon_date IS NULL
            AND base_currency IS NOT NULL
            AND quote_currency IS NOT NULL
            AND base_currency <> quote_currency
            AND currency = quote_currency)
    );
