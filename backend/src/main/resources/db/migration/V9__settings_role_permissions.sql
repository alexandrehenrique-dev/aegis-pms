CREATE TABLE role_permissions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    permission_key VARCHAR(160) NOT NULL,
    allowed BOOLEAN NOT NULL,
    CONSTRAINT uq_role_permissions_tenant_role_key UNIQUE (tenant_id, role, permission_key)
);

CREATE INDEX idx_role_permissions_tenant_id ON role_permissions (tenant_id);
CREATE INDEX idx_role_permissions_tenant_role ON role_permissions (tenant_id, role);
