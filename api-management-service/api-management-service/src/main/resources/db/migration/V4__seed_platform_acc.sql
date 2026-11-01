INSERT INTO management_users(
    organization_id, role_id, username, password_hash, created_at, updated_at
)
SELECT NULL, roles.id, 'platform-admin1',
       '$2y$12$OmOK.rj8fuLD2oZ5Uimd5.vNlqPUZvwTZcLSyzmACKDX/qq8f3Qq2',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE roles.name = 'PLATFORM_ADMIN';

INSERT INTO management_users(
    organization_id, role_id, username, password_hash, created_at, updated_at
)
SELECT NULL, roles.id, 'gateway-service1',
       '$2y$12$noOqk6RMaZvaZaOpk4mXOeTO5GVFPuAest7kJDaKoe0Dt0AQYMelS',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE roles.name = 'GATEWAY_SERVICE';
