ALTER TABLE form_definitions
    ADD COLUMN response_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_activity_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE product_security_settings (
    product_id UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    webhook_url VARCHAR(1000),
    webhook_secret VARCHAR(500),
    analytics_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    analytics_provider_key VARCHAR(500),
    email_delivery_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE
);
