CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    from_account VARCHAR(20) NOT NULL,
    to_account VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    fee DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_from ON transactions(from_account);
CREATE INDEX idx_transactions_to ON transactions(to_account);
CREATE INDEX idx_transactions_status ON transactions(status);