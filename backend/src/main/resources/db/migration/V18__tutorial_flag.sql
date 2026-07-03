ALTER TABLE tenant_memberships ADD COLUMN tutorial_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant_memberships ADD COLUMN tutorial_completed_at TIMESTAMPTZ;
