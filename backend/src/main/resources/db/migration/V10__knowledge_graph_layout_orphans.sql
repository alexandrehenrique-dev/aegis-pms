ALTER TABLE graph_nodes
    ADD COLUMN x DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN y DOUBLE PRECISION NOT NULL DEFAULT 0;

CREATE TABLE graph_insight_reviews (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL REFERENCES products(id),
    text_hash VARCHAR(64) NOT NULL,
    text VARCHAR(1000) NOT NULL,
    reviewed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_graph_insight_review_product_hash UNIQUE (product_id, text_hash)
);

CREATE INDEX idx_graph_insight_reviews_tenant_id ON graph_insight_reviews (tenant_id);
CREATE INDEX idx_graph_insight_reviews_product_id ON graph_insight_reviews (product_id);
