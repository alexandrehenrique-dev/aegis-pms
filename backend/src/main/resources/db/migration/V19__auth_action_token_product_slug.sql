-- Adiciona o slug do produto ao token de convite para que o frontend possa
-- redirecionar o usuário diretamente ao produto após a ativação.
-- Coluna nullable: tokens de convite via tenant-level (TenantUserService) não
-- possuem produto específico e permanecerão com NULL.
ALTER TABLE auth_action_tokens
    ADD COLUMN product_slug VARCHAR(120);
