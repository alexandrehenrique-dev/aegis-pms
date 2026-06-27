CREATE TABLE contents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    type VARCHAR(80) NOT NULL,
    lang VARCHAR(16) NOT NULL,
    author_subject VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    publication VARCHAR(160),
    current_version INTEGER NOT NULL DEFAULT 1,
    summary VARCHAR(480),
    difficulty_level VARCHAR(20),
    body_markdown TEXT,
    category VARCHAR(160),
    topic VARCHAR(160),
    metadata_json JSONB,
    graph_node_id UUID REFERENCES graph_nodes(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE content_versions (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    version_label VARCHAR(20) NOT NULL,
    snapshot_json JSONB NOT NULL,
    created_by_subject VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_contents_tenant_id ON contents (tenant_id);
CREATE INDEX idx_contents_product_id ON contents (product_id);
CREATE INDEX idx_contents_status ON contents (status);

CREATE INDEX idx_content_versions_content_id ON content_versions (content_id);
