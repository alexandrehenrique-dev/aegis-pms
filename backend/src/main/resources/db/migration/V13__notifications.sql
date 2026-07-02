CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    body_markdown TEXT NOT NULL,
    presentation_mode VARCHAR(40) NOT NULL,
    created_by_subject VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_notification_statuses (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_subject VARCHAR(160) NOT NULL,
    auto_shown BOOLEAN NOT NULL DEFAULT FALSE,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    shown_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_notification_status UNIQUE (notification_id, user_subject)
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);
CREATE INDEX idx_user_notification_statuses_user_subject ON user_notification_statuses (user_subject);
CREATE INDEX idx_user_notification_statuses_notification_id ON user_notification_statuses (notification_id);
CREATE INDEX idx_user_notification_statuses_pending_modal
    ON user_notification_statuses (user_subject, auto_shown, notification_id);
CREATE INDEX idx_user_notification_statuses_user_created
    ON user_notification_statuses (user_subject, created_at DESC);

INSERT INTO notifications (
    id,
    type,
    title,
    body_markdown,
    presentation_mode,
    created_by_subject,
    created_at,
    updated_at
) VALUES (
    '25000000-0000-0000-0000-000000000001',
    'ONBOARDING',
    'Bem-vindo ao Aegis',
    '<p>Bem-vindo ao Aegis. Use este painel para administrar produtos digitais, acompanhar notificações e organizar sua operação.</p>',
    'MODAL_ONCE',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
