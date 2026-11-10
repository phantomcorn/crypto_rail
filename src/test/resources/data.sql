INSERT INTO invoices (
    id, hash_id, merchant_address, token_address, amount,
    app_name, app_version, chain_id,
    signature, signature_expires_at, status
) VALUES (
    '3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3a',
    'eeed045bdfccb92acda79f2d11af98124a8506a7a3300f13b869eaf0ede0e9b0',                         -- 66 chars: 0x + 64 hex
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

INSERT INTO invoices (
    id, hash_id, merchant_address, token_address, amount,
    app_name, app_version, chain_id,
    signature, signature_expires_at, status
) VALUES (
    '3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3b',
    'e285fbc3018ed2aded18af5940c437d352f0c8235c5449e2473588cf11a5cc08',                         -- 66 chars: 0x + 64 hex
    '0x1111111111111111111111111111111111111111',     -- merchant
    '0x2222222222222222222222222222222222222222',     -- token
    1000000,                                          -- e.g. 1 USDC (6 decimals)
    'CryptoRail',
    '0.0.1',
    11155111,                                         -- Sepolia
    '0x' || repeat('cd', 65),                         -- 132 chars: 0x + 130 hex
    1761300000,                                       -- unix deadline
    'DETECTED'
);

INSERT INTO invoices (
    id, hash_id, merchant_address, token_address, amount,
    app_name, app_version, chain_id,
    signature, signature_expires_at, status
) VALUES (
    '3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3c',
    '6ecb18f2c7a57f89f942414028ffd2a4aef7528e242d34bbece14c6e77a52186',                         -- 66 chars: 0x + 64 hex
    '0x1111111111111111111111111111111111111111',     -- merchant
    '0x2222222222222222222222222222222222222222',     -- token
    1000000,                                          -- e.g. 1 USDC (6 decimals)
    'CryptoRail',
    '0.0.1',
    11155111,                                         -- Sepolia
    '0x' || repeat('cd', 65),                         -- 132 chars: 0x + 130 hex
    1761300000,                                       -- unix deadline
    'REORG'
);

INSERT INTO invoices (
    id, hash_id, merchant_address, token_address, amount,
    app_name, app_version, chain_id,
    signature, signature_expires_at, status
) VALUES (
    '3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3d',
    'f933631c3fae26d8eec0f0317f770095071dabb50ce4ea8e7ccafdd976ba187f',                         -- 66 chars: 0x + 64 hex
    '0x1111111111111111111111111111111111111111',     -- merchant
    '0x2222222222222222222222222222222222222222',     -- token
    1000000,                                          -- e.g. 1 USDC (6 decimals)
    'CryptoRail',
    '0.0.1',
    11155111,                                         -- Sepolia
    '0x' || repeat('cd', 65),                         -- 132 chars: 0x + 130 hex
    1761300000,                                       -- unix deadline
    'CONFIRMED'
);

INSERT INTO merchant (
    address,
    name,
    uri
) VALUES (
    '0x1111111111111111111111111111111111111111',
    'Merchant A',
    'https://www.google.com'
);