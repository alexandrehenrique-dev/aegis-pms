CREATE SEQUENCE feedback_public_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE feedback (
    id UUID PRIMARY KEY,
    public_id VARCHAR(16) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    created_by_subject VARCHAR(160) NOT NULL,
    category VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    screen_name VARCHAR(240),
    attachment_asset_id UUID REFERENCES assets(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_feedback_public_id UNIQUE (public_id),
    CONSTRAINT ck_feedback_category CHECK (
        category IN (
            'BUG',
            'CONFUSING_UX',
            'VISUAL_ERROR',
            'INCORRECT_PERMISSION',
            'WRONG_INFORMATION',
            'SUGGESTION'
        )
    ),
    CONSTRAINT ck_feedback_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_feedback_status CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED')),
    CONSTRAINT ck_feedback_public_id_format CHECK (public_id ~ '^AGS-[0-9]{4,}$')
);

CREATE INDEX idx_feedback_tenant_id ON feedback (tenant_id);
CREATE INDEX idx_feedback_status ON feedback (status);
CREATE INDEX idx_feedback_tenant_status_created ON feedback (tenant_id, status, created_at DESC);
CREATE INDEX idx_feedback_tenant_created ON feedback (tenant_id, created_at DESC);
CREATE INDEX idx_feedback_created_at ON feedback (created_at DESC);
