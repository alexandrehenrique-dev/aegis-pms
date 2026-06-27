ALTER TABLE audit_events DROP CONSTRAINT audit_events_tenant_id_fkey;
ALTER TABLE audit_events DROP CONSTRAINT audit_events_product_id_fkey;

ALTER TABLE audit_events RENAME COLUMN actor_user_id TO actor_subject;
ALTER TABLE audit_events ALTER COLUMN actor_subject TYPE VARCHAR(160) USING actor_subject::text;

ALTER TABLE audit_events RENAME COLUMN entity_type TO target_type;

ALTER TABLE audit_events RENAME COLUMN entity_id TO target_id;
ALTER TABLE audit_events ALTER COLUMN target_id TYPE VARCHAR(160) USING target_id::text;

ALTER TABLE audit_events RENAME COLUMN event_type TO action;

ALTER TABLE audit_events RENAME COLUMN payload TO diff_json;
ALTER TABLE audit_events ALTER COLUMN diff_json DROP NOT NULL;
ALTER TABLE audit_events ALTER COLUMN diff_json DROP DEFAULT;

ALTER TABLE audit_events RENAME COLUMN occurred_at TO created_at;

ALTER TABLE audit_events ADD COLUMN target_label VARCHAR(255);
ALTER TABLE audit_events ADD COLUMN module VARCHAR(80);
ALTER TABLE audit_events ADD COLUMN risk VARCHAR(20) NOT NULL DEFAULT 'BAIXO';
ALTER TABLE audit_events ADD COLUMN trace_id VARCHAR(80);
ALTER TABLE audit_events ADD COLUMN ip VARCHAR(64);
ALTER TABLE audit_events ADD COLUMN user_agent VARCHAR(512);

UPDATE audit_events SET actor_subject = 'unknown' WHERE actor_subject IS NULL;
ALTER TABLE audit_events ALTER COLUMN actor_subject SET NOT NULL;

CREATE INDEX idx_audit_events_tenant_id ON audit_events (tenant_id);
CREATE INDEX idx_audit_events_action ON audit_events (action);
CREATE INDEX idx_audit_events_module ON audit_events (module);
CREATE INDEX idx_audit_events_risk ON audit_events (risk);
