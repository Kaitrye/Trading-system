CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE instruments (
    seccode VARCHAR(16) PRIMARY KEY,
    type VARCHAR(20) NOT NULL DEFAULT 'STOCK',
    lot_size INTEGER NOT NULL,
    min_step NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'RUB',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_instruments_seccode CHECK (seccode = upper(seccode)),
    CONSTRAINT ck_instruments_type CHECK (type = 'STOCK'),
    CONSTRAINT ck_instruments_lot_size CHECK (lot_size > 0),
    CONSTRAINT ck_instruments_min_step CHECK (min_step > 0)
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    currency CHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reserved_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_accounts_user_currency UNIQUE (user_id, currency),
    CONSTRAINT ck_accounts_balance CHECK (balance >= reserved_balance AND reserved_balance >= 0)
);

CREATE TABLE positions (
    account_id UUID NOT NULL REFERENCES accounts (id),
    seccode VARCHAR(16) NOT NULL REFERENCES instruments (seccode),
    quantity BIGINT NOT NULL DEFAULT 0,
    reserved_quantity BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id, seccode),
    CONSTRAINT ck_positions_quantity CHECK (quantity >= reserved_quantity AND reserved_quantity >= 0)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    seccode VARCHAR(16) NOT NULL REFERENCES instruments (seccode),
    side VARCHAR(4) NOT NULL,
    price NUMERIC(19, 4) NOT NULL,
    quantity BIGINT NOT NULL,
    remaining_quantity BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_orders_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT ck_orders_price CHECK (price > 0),
    CONSTRAINT ck_orders_quantity CHECK (quantity > 0),
    CONSTRAINT ck_orders_remaining_quantity CHECK (remaining_quantity >= 0 AND remaining_quantity <= quantity),
    CONSTRAINT ck_orders_status CHECK (status IN ('NEW', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED'))
);

CREATE TABLE trades (
    id UUID PRIMARY KEY,
    seccode VARCHAR(16) NOT NULL REFERENCES instruments (seccode),
    price NUMERIC(19, 4) NOT NULL,
    quantity BIGINT NOT NULL,
    buy_order_id UUID NOT NULL REFERENCES orders (id),
    sell_order_id UUID NOT NULL REFERENCES orders (id),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_trades_price CHECK (price > 0),
    CONSTRAINT ck_trades_quantity CHECK (quantity > 0),
    CONSTRAINT ck_trades_different_orders CHECK (buy_order_id <> sell_order_id)
);

CREATE TABLE account_operations (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    trade_id UUID REFERENCES trades (id),
    operation_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_account_operations_amount_not_zero CHECK (amount <> 0)
);

CREATE INDEX idx_orders_active_book
    ON orders (seccode, side, price, created_at)
    WHERE status IN ('NEW', 'PARTIALLY_FILLED');

CREATE INDEX idx_orders_account_created_at
    ON orders (account_id, created_at DESC);

CREATE INDEX idx_trades_buy_order
    ON trades (buy_order_id);

CREATE INDEX idx_trades_sell_order
    ON trades (sell_order_id);

CREATE INDEX idx_trades_seccode_executed_at
    ON trades (seccode, executed_at DESC);

CREATE INDEX idx_account_operations_account_created_at
    ON account_operations (account_id, created_at DESC);
