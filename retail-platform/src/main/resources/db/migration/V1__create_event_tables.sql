CREATE TABLE processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMPTZ,
    retailer_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100),
    product_id VARCHAR(100),
    campaign_id VARCHAR(100),
    ad_id VARCHAR(100),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_campaign_time
    ON processed_events (campaign_id, event_time);

CREATE TABLE campaign_metrics (
    campaign_id VARCHAR(100) PRIMARY KEY,
    impressions BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    basket_additions BIGINT NOT NULL DEFAULT 0,
    product_views BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);