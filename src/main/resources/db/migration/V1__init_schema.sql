CREATE TABLE instruments (
                             seccode   VARCHAR(32)    PRIMARY KEY,
                             type      VARCHAR(16)    NOT NULL,
                             lot_size  INTEGER        NOT NULL,
                             min_step  NUMERIC(19, 4) NOT NULL,
                             CONSTRAINT chk_instruments_type
                                 CHECK (type IN ('STOCK', 'BOND', 'CURRENCY')),
                             CONSTRAINT chk_instruments_lot_size
                                 CHECK (lot_size > 0),
                             CONSTRAINT chk_instruments_min_step
                                 CHECK (min_step > 0)
);

CREATE TABLE accounts (
                          id                BIGSERIAL      PRIMARY KEY,
                          user_id           BIGINT         NOT NULL,
                          balance           NUMERIC(19, 4) NOT NULL DEFAULT 0,
                          reserved_balance  NUMERIC(19, 4) NOT NULL DEFAULT 0,
                          currency          VARCHAR(3)     NOT NULL,
                          version           BIGINT         NOT NULL DEFAULT 0,
                          CONSTRAINT chk_accounts_balance_non_negative
                              CHECK (balance >= 0),
                          CONSTRAINT chk_accounts_reserved_non_negative
                              CHECK (reserved_balance >= 0),
                          CONSTRAINT chk_accounts_reserved_le_balance
                              CHECK (reserved_balance <= balance)
);

CREATE INDEX ix_accounts_user_id ON accounts (user_id);

CREATE TABLE holdings (
                          id                 BIGSERIAL PRIMARY KEY,
                          account_id         BIGINT    NOT NULL,
                          seccode            VARCHAR(32) NOT NULL,
                          quantity           BIGINT    NOT NULL DEFAULT 0,
                          reserved_quantity  BIGINT    NOT NULL DEFAULT 0,
                          version            BIGINT    NOT NULL DEFAULT 0,
                          CONSTRAINT uq_holdings_account_seccode UNIQUE (account_id, seccode),
                          CONSTRAINT fk_holdings_account
                              FOREIGN KEY (account_id) REFERENCES accounts (id),
                          CONSTRAINT fk_holdings_instrument
                              FOREIGN KEY (seccode) REFERENCES instruments (seccode),
                          CONSTRAINT chk_holdings_quantity_non_negative
                              CHECK (quantity >= 0),
                          CONSTRAINT chk_holdings_reserved_non_negative
                              CHECK (reserved_quantity >= 0),
                          CONSTRAINT chk_holdings_reserved_le_quantity
                              CHECK (reserved_quantity <= quantity)
);

CREATE TABLE orders (
                        id                BIGSERIAL      PRIMARY KEY,
                        seccode           VARCHAR(32)    NOT NULL,
                        type              VARCHAR(8)     NOT NULL,
                        price             NUMERIC(19, 4) NOT NULL,
                        quantity          BIGINT         NOT NULL,
                        filled_quantity   BIGINT         NOT NULL DEFAULT 0,
                        status            VARCHAR(16)    NOT NULL,
                        account_id        BIGINT         NOT NULL,
                        created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
                        version           BIGINT         NOT NULL DEFAULT 0,
                        CONSTRAINT fk_orders_instrument
                            FOREIGN KEY (seccode) REFERENCES instruments (seccode),
                        CONSTRAINT fk_orders_account
                            FOREIGN KEY (account_id) REFERENCES accounts (id),
                        CONSTRAINT chk_orders_type
                            CHECK (type IN ('BUY', 'SELL')),
                        CONSTRAINT chk_orders_status
                            CHECK (status IN ('NEW', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED')),
                        CONSTRAINT chk_orders_price_positive
                            CHECK (price > 0),
                        CONSTRAINT chk_orders_quantity_positive
                            CHECK (quantity > 0),
                        CONSTRAINT chk_orders_filled_range
                            CHECK (filled_quantity >= 0 AND filled_quantity <= quantity)
);

-- тот самый индекс, что в @Table(indexes = ...) на TradeOrder
CREATE INDEX ix_orders_seccode_type_status
    ON orders (seccode, type, status, price);


CREATE INDEX ix_orders_account_created
    ON orders (account_id, created_at DESC);

CREATE TABLE trades (
                        id             BIGSERIAL      PRIMARY KEY,
                        seccode        VARCHAR(32)    NOT NULL,
                        price          NUMERIC(19, 4) NOT NULL,
                        quantity       BIGINT         NOT NULL,
                        buy_order_id   BIGINT         NOT NULL,
                        sell_order_id  BIGINT         NOT NULL,
                        executed_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
                        CONSTRAINT fk_trades_instrument
                            FOREIGN KEY (seccode) REFERENCES instruments (seccode),
                        CONSTRAINT fk_trades_buy_order
                            FOREIGN KEY (buy_order_id) REFERENCES orders (id),
                        CONSTRAINT fk_trades_sell_order
                            FOREIGN KEY (sell_order_id) REFERENCES orders (id),
                        CONSTRAINT chk_trades_price_positive
                            CHECK (price > 0),
                        CONSTRAINT chk_trades_quantity_positive
                            CHECK (quantity > 0)
);

CREATE INDEX ix_trades_seccode_executed
    ON trades (seccode, executed_at DESC);
CREATE INDEX ix_trades_buy_order  ON trades (buy_order_id);
CREATE INDEX ix_trades_sell_order ON trades (sell_order_id);