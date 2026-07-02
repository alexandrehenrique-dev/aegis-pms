CREATE TABLE pages (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    slug VARCHAR(160) NOT NULL,
    title VARCHAR(240) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version INTEGER NOT NULL,
    seo_title VARCHAR(240),
    seo_description VARCHAR(480),
    seo_canonical VARCHAR(480),
    seo_og_image_asset_id UUID,
    seo_no_index BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_pages_product_slug UNIQUE (product_id, slug)
);

CREATE INDEX idx_pages_tenant_id ON pages (tenant_id);
CREATE INDEX idx_pages_product_id ON pages (product_id);
CREATE INDEX idx_pages_status ON pages (status);

CREATE TABLE page_sections (
    id UUID PRIMARY KEY,
    page_id UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    variant VARCHAR(80),
    section_order INTEGER NOT NULL,
    content_json JSONB NOT NULL,
    settings_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_page_sections_page_id ON page_sections (page_id);
CREATE INDEX idx_page_sections_page_id_order ON page_sections (page_id, section_order);

CREATE TABLE product_globals (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    navbar_json JSONB NOT NULL,
    footer_json JSONB NOT NULL,
    social_links_json JSONB NOT NULL,
    floating_whatsapp_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_product_globals_product UNIQUE (product_id)
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    datetime TIMESTAMP NOT NULL,
    location VARCHAR(240) NOT NULL,
    type VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    description TEXT,
    image_asset_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_events_tenant_id ON events (tenant_id);
CREATE INDEX idx_events_product_id ON events (product_id);
CREATE INDEX idx_events_product_id_datetime ON events (product_id, datetime);
