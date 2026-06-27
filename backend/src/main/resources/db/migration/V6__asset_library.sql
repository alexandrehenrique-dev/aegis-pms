CREATE TABLE assets (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    friendly_name VARCHAR(255),
    alt_text VARCHAR(480),
    caption VARCHAR(480),
    credit VARCHAR(255),
    mime_type VARCHAR(120) NOT NULL,
    category VARCHAR(20) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    uploaded_by_subject VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_assets_tenant_id ON assets (tenant_id);
CREATE INDEX idx_assets_product_id ON assets (product_id);
CREATE INDEX idx_assets_category ON assets (category);

CREATE TABLE asset_tags (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_asset_tags_product_id_name UNIQUE (product_id, name)
);

CREATE INDEX idx_asset_tags_product_id ON asset_tags (product_id);

CREATE TABLE asset_tag_assignments (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    asset_tag_id UUID NOT NULL REFERENCES asset_tags(id) ON DELETE CASCADE,
    CONSTRAINT uk_asset_tag_assignments_asset_id_tag_id UNIQUE (asset_id, asset_tag_id)
);

CREATE INDEX idx_asset_tag_assignments_asset_id ON asset_tag_assignments (asset_id);
CREATE INDEX idx_asset_tag_assignments_asset_tag_id ON asset_tag_assignments (asset_tag_id);

CREATE TABLE asset_usages (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    used_in_type VARCHAR(80) NOT NULL,
    used_in_ref_id VARCHAR(160) NOT NULL,
    used_in_label VARCHAR(255) NOT NULL
);

CREATE INDEX idx_asset_usages_asset_id ON asset_usages (asset_id);
