CREATE TABLE form_definitions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fields_json JSONB NOT NULL,
    delivery_channels_json JSONB NOT NULL,
    publication VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_form_definitions_tenant_id ON form_definitions (tenant_id);
CREATE INDEX idx_form_definitions_product_id ON form_definitions (product_id);
CREATE INDEX idx_form_definitions_status ON form_definitions (status);

CREATE TABLE form_submissions (
    id UUID PRIMARY KEY,
    form_id UUID NOT NULL REFERENCES form_definitions(id) ON DELETE CASCADE,
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL,
    source VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    owner_subject VARCHAR(160),
    score INTEGER,
    answers_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_form_submissions_form_id ON form_submissions (form_id);
CREATE INDEX idx_form_submissions_date ON form_submissions (date);
CREATE INDEX idx_form_submissions_status ON form_submissions (status);
