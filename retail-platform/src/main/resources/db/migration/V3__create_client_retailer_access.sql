CREATE TABLE client_retailer_access (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(150) NOT NULL,
    retailer_id VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_client_retailer
        UNIQUE (client_id, retailer_id)
);

CREATE INDEX idx_client_retailer_access_lookup
    ON client_retailer_access (client_id, retailer_id, enabled);