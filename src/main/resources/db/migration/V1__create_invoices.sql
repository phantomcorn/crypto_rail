CREATE TYPE invoice_status AS ENUM ('NOT_PAID', 'DETECTED', 'REORG', 'CONFIRMED');

CREATE TABLE merchant(
     username VARCHAR(256) PRIMARY KEY,
     password VARCHAR(256) NOT NULL,
     address VARCHAR(42) NOT NULL,
     name VARCHAR(256) NOT NULL,
     uri VARCHAR(256) NOT NULL,
     secret_key VARCHAR(44) NOT NULL
);

CREATE TABLE invoices(
    id VARCHAR(64) PRIMARY KEY,
    hash_id VARCHAR(66) NOT NULL UNIQUE,
    merchant_address VARCHAR(42) NOT NULL,
    token_address VARCHAR(42) NOT NULL,
    amount numeric(64, 0) NOT NULL,
    app_name VARCHAR(256) NOT NULL,
    app_version VARCHAR(10) NOT NULL,
    chain_id BIGINT NOT NULL,
    signature VARCHAR(132) NOT NULL,
    signature_expires_at numeric(64,0) NOT NULL,
    status invoice_status NOT NULL DEFAULT 'NOT_PAID',
    merchant_username VARCHAR(256) NOT NULL,
    FOREIGN KEY (merchant_username) REFERENCES merchant(username)
);

CREATE TABLE invoice_payments(
    invoice_hash_id VARCHAR(66) PRIMARY KEY REFERENCES invoices(hash_id) ON DELETE CASCADE,
    block_number numeric(64,0) NOT NULL,
    block_hash VARCHAR(66) NOT NULL
);

CREATE TABLE api_keys(
    hash_key VARCHAR(66) PRIMARY KEY,
    label VARCHAR(256) NOT NULL,
    merchant_username VARCHAR(256) NOT NULL,
    FOREIGN KEY (merchant_username) REFERENCES merchant(username)
)