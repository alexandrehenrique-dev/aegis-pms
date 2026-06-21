CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL,
    role VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_tenant_membership_user UNIQUE (tenant_id, user_id)
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_product_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE product_modules (
     id UUID PRIMARY KEY,
     product_id UUID NOT NULL REFERENCES products(id),
     module_key VARCHAR(80) NOT NULL,
     enabled BOOLEAN NOT NULL DEFAULT TRUE,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
     CONSTRAINT uq_product_module UNIQUE (product_id, module_key)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID REFERENCES tenants(id),
    product_id UUID REFERENCES products(id),
    actor_user_id UUID,
    event_type VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120),
    entity_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    completion_date TIMESTAMP(9),
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP(9) NOT NULL,
    serialized_event VARCHAR(4000) NOT NULL,
    status VARCHAR(20),
    completion_attempts INT,
    last_resubmission_date TIMESTAMP(9)
);

CREATE INDEX event_publication_by_listener_id_and_serialized_event_idx
    ON event_publication (listener_id, serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
    ON event_publication (completion_date);