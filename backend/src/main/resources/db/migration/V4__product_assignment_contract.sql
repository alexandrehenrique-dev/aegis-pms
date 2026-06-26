ALTER TABLE tenants ADD COLUMN plan VARCHAR(80) NOT NULL DEFAULT 'FREE';

ALTER TABLE product_assignments ADD COLUMN tenant_id UUID;
UPDATE product_assignments pa
SET tenant_id = p.tenant_id
FROM products p
WHERE pa.product_id = p.id;
ALTER TABLE product_assignments ALTER COLUMN tenant_id SET NOT NULL;
UPDATE product_assignments SET status = 'INVITED' WHERE status = 'REVOKED';

ALTER TABLE tenant_memberships DROP CONSTRAINT tenant_memberships_tenant_id_fkey;
ALTER TABLE tenant_memberships
    ADD CONSTRAINT tenant_memberships_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE product_modules DROP CONSTRAINT product_modules_product_id_fkey;
ALTER TABLE product_modules
    ADD CONSTRAINT product_modules_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE product_assignments DROP CONSTRAINT product_assignments_product_id_fkey;
ALTER TABLE product_assignments
    ADD CONSTRAINT product_assignments_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE product_assignments
    ADD CONSTRAINT product_assignments_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE products DROP CONSTRAINT products_tenant_id_fkey;
ALTER TABLE products
    ADD CONSTRAINT products_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_product_assignments_tenant_id ON product_assignments (tenant_id);
