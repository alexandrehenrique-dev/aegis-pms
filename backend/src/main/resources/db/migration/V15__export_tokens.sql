CREATE TABLE export_tokens (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    product_key VARCHAR(120) NOT NULL,
    zip_path VARCHAR(1024) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    downloaded_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_export_tokens_product_id ON export_tokens (product_id);
CREATE INDEX idx_export_tokens_status_expires_at ON export_tokens (status, expires_at);
