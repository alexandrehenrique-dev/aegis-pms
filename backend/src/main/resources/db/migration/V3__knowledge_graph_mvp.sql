CREATE TABLE graph_nodes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL REFERENCES products(id),
    node_type VARCHAR(80) NOT NULL,
    ref_type VARCHAR(120) NOT NULL,
    ref_id VARCHAR(160) NOT NULL,
    label VARCHAR(240) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_graph_node_product_ref UNIQUE (product_id, ref_type, ref_id)
);

CREATE TABLE graph_edges (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL REFERENCES products(id),
    source_node_id UUID NOT NULL REFERENCES graph_nodes(id),
    target_node_id UUID NOT NULL REFERENCES graph_nodes(id),
    edge_type VARCHAR(80) NOT NULL,
    weight NUMERIC(10, 4) NOT NULL DEFAULT 1,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_graph_edge_source_target_type UNIQUE (source_node_id, target_node_id, edge_type)
);

CREATE INDEX idx_graph_nodes_tenant_id ON graph_nodes (tenant_id);
CREATE INDEX idx_graph_nodes_product_id ON graph_nodes (product_id);
CREATE INDEX idx_graph_nodes_node_type ON graph_nodes (node_type);
CREATE INDEX idx_graph_nodes_ref_type_ref_id ON graph_nodes (ref_type, ref_id);

CREATE INDEX idx_graph_edges_tenant_id ON graph_edges (tenant_id);
CREATE INDEX idx_graph_edges_product_id ON graph_edges (product_id);
CREATE INDEX idx_graph_edges_source_node_id ON graph_edges (source_node_id);
CREATE INDEX idx_graph_edges_target_node_id ON graph_edges (target_node_id);
CREATE INDEX idx_graph_edges_edge_type ON graph_edges (edge_type);
