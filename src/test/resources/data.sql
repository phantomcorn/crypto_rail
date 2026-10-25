INSERT INTO invoices (
    id, hash_id, merchant_address, token_address, amount,
    app_name, app_version, chain_id,
    signature, signature_expires_at, status
) VALUES (
    '3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3c',
    '0x' || repeat('ab', 32),                         -- 66 chars: 0x + 64 hex
    '0x1111111111111111111111111111111111111111',     -- merchant
    '0x2222222222222222222222222222222222222222',     -- token
    1000000,                                          -- e.g. 1 USDC (6 decimals)
    'CryptoRail',
    '0.0.1',
    11155111,                                         -- Sepolia
    '0x' || repeat('cd', 65),                         -- 132 chars: 0x + 130 hex
    1761300000,                                       -- unix deadline
    'NOT_PAID'
);