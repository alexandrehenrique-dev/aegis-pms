CREATE TABLE auth_action_tokens (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    type VARCHAR(20) NOT NULL CHECK (type IN ('INVITE', 'PASSWORD_RESET')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'USED', 'EXPIRED')),
    tenant_id UUID,
    tenant_name VARCHAR(255),
    product_names TEXT,
    role VARCHAR(50),
    inviter_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_aat_status_expires ON auth_action_tokens (status, expires_at);
CREATE INDEX idx_aat_email_type_created ON auth_action_tokens (user_email, type, created_at);
