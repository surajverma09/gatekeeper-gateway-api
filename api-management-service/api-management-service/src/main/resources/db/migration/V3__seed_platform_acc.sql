INSERT INTO management_users(
    organization_id, role_id, username, password_hash, created_at, updated_at
)
SELECT NULL, roles.id, 'platform-admin',
       'b378eba8aba0c8147c2609f16336ad389a28eb9a6c1f9c47137ad301f0182576',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE roles.name = 'PLATFORM_ADMIN';

INSERT INTO management_users(
    organization_id, role_id, username, password_hash, created_at, updated_at
)
SELECT NULL, roles.id, 'gateway-service',
       'd70fe535b0097ea79781a359cd041aff9bf55aab6b2d59a057aeec343b0b4e3e',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE roles.name = 'GATEWAY_SERVICE';

