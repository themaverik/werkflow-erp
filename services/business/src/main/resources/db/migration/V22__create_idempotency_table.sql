CREATE TABLE IF NOT EXISTS idempotency_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_payload TEXT,
    response_body TEXT,
    response_headers TEXT,
    status_code INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_idempotency_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_idempotency_tenant_created ON idempotency_record(tenant_id, created_at);
