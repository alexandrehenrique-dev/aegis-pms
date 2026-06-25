ALTER TABLE tenants RENAME COLUMN slug TO key;
ALTER TABLE tenants RENAME CONSTRAINT tenants_slug_key TO tenants_key_key;
ALTER TABLE tenants ALTER COLUMN key SET NOT NULL;

ALTER TABLE tenant_memberships DROP CONSTRAINT uq_tenant_membership_user;
ALTER TABLE tenant_memberships RENAME COLUMN user_id TO user_subject;
ALTER TABLE tenant_memberships ALTER COLUMN user_subject TYPE VARCHAR(160) USING user_subject::text;
ALTER TABLE tenant_memberships ALTER COLUMN user_subject SET NOT NULL;
ALTER TABLE tenant_memberships ADD CONSTRAINT uq_tenant_membership_user_subject UNIQUE (tenant_id, user_subject);

ALTER TABLE products RENAME COLUMN slug TO key;
ALTER TABLE products RENAME CONSTRAINT uq_product_tenant_slug TO uq_product_tenant_key;
ALTER TABLE products ALTER COLUMN key SET NOT NULL;
ALTER TABLE products ADD COLUMN type VARCHAR(80) NOT NULL DEFAULT 'CUSTOM';
ALTER TABLE products ADD COLUMN default_locale VARCHAR(16) NOT NULL DEFAULT 'pt-BR';
ALTER TABLE products ADD COLUMN asset_storage_strategy VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE products ALTER COLUMN type DROP DEFAULT;
ALTER TABLE products ALTER COLUMN default_locale DROP DEFAULT;
ALTER TABLE products ALTER COLUMN asset_storage_strategy DROP DEFAULT;

ALTER TABLE product_modules ADD COLUMN settings_json JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE TABLE product_assignments (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    user_subject VARCHAR(160) NOT NULL,
    role VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_product_assignment_user_subject UNIQUE (product_id, user_subject)
);

CREATE INDEX idx_tenant_memberships_user_subject ON tenant_memberships (user_subject);
CREATE INDEX idx_products_tenant_id ON products (tenant_id);
CREATE INDEX idx_product_modules_product_id ON product_modules (product_id);
CREATE INDEX idx_product_assignments_user_subject ON product_assignments (user_subject);
CREATE INDEX idx_product_assignments_product_id ON product_assignments (product_id);
