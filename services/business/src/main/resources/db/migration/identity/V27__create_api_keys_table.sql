CREATE TABLE api_keys (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,
    tenant_id   VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ
);

-- UNIQUE on key_hash already creates an implicit index; only tenant lookup needs an explicit one
CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);
