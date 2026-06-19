# Etapa 03 — Keycloak persistente, realm/client/roles e export

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 02 concluída (os dois PostgreSQL no ar).

## Contexto fixo

Aegis PMS usa Keycloak como IAM (ver ADR-0005). Regra inegociável: o Keycloak **precisa** estar conectado ao `keycloak-postgres` da etapa anterior — nunca ao banco H2 interno de desenvolvimento. Usuários, realm e clients devem sobreviver a qualquer restart ou `docker compose down/up` sem `-v`.

## Objetivo

Subir o Keycloak conectado ao PostgreSQL dedicado, configurar o realm `aegis` com o client do frontend e as roles globais, e exportar o realm para ficar versionado no repositório.

## Tarefas

### A. Serviço Keycloak

Adicionar ao `docker-compose.yml`:

```yaml
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    container_name: aegis-keycloak
    restart: unless-stopped
    command: start-dev
    environment:
      KC_DB: postgres
      KC_DB_URL_HOST: keycloak-postgres
      KC_DB_URL_PORT: 5432
      KC_DB_URL_DATABASE: ${KEYCLOAK_DB_NAME}
      KC_DB_USERNAME: ${KEYCLOAK_DB_USER}
      KC_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KC_HTTP_ENABLED: "true"
      KC_HOSTNAME_STRICT: "false"
      KC_HOSTNAME_STRICT_HTTPS: "false"
    ports:
      - "${KEYCLOAK_HTTP_PORT}:8080"
    depends_on:
      keycloak-postgres:
        condition: service_healthy
    networks:
      - aegis-network
```

### B. Realm, client e roles

Subir e configurar manualmente via console admin (`http://localhost:8282`, login `admin`/`admin`):

- Criar realm `aegis`.
- Criar client `aegis-web`: tipo OpenID Connect, Client authentication OFF, Standard flow ON, Direct access grants OFF (a menos que precise testar temporariamente), PKCE S256 se disponível.
  - Valid redirect URIs: `http://localhost:5173/*`, `http://localhost:8080/*` (5173 = Vite; 8080 = quando o backend já estiver servindo o build do React).
  - Web origins: `http://localhost:5173`, `http://localhost:8080`.
- Criar roles globais: `AEGIS_SUPER_ADMIN`, `TENANT_ADMIN`, `PRODUCT_MANAGER`, `CONTENT_EDITOR`, `CONTENT_REVIEWER`, `VIEWER`.
- Criar um usuário de teste com uma dessas roles, e-mail verificado.

Observação arquitetural: Keycloak autentica; o Aegis autoriza operacionalmente por tenant/membership/módulo (isso será implementado na etapa 06). As roles do Keycloak ficam amplas — permissões finas pertencem ao backend.

### C. Exportar o realm para versionamento

```bash
mkdir -p infra/keycloak/realm

docker exec -it aegis-keycloak /opt/keycloak/bin/kc.sh export \
  --dir /tmp/keycloak-export \
  --realm aegis \
  --users realm_file

docker cp aegis-keycloak:/tmp/keycloak-export/aegis-realm.json ./infra/keycloak/realm/aegis-realm.json
```

Esse arquivo **não** entra no `.gitignore` — configuração de Keycloak é versionada (apenas dados/volumes locais ficam de fora).

## Critérios de aceite

- [ ] Keycloak acessível em `http://localhost:8282`, login admin funciona.
- [ ] Keycloak usa o `keycloak-postgres`, não H2.
- [ ] Realm `aegis`, client `aegis-web` e as 6 roles globais existem.
- [ ] Usuário de teste criado e com role atribuída.
- [ ] Realm e usuário sobrevivem a `docker compose restart keycloak` e a `docker compose down && docker compose up -d` (sem `-v`).
- [ ] `infra/keycloak/realm/aegis-realm.json` existe e está versionado.

## Validação

```bash
curl http://localhost:8282/realms/aegis/.well-known/openid-configuration
# deve retornar JSON com issuer = http://localhost:8282/realms/aegis

docker exec -it keycloak-postgres psql -U keycloak_user -d keycloak -c "SELECT count(*) FROM realm;"
# esperado: count >= 1

docker compose restart keycloak
# revalidar login admin e presença do realm/usuário
```

## Commit sugerido

```bash
git add docker-compose.yml infra/keycloak/realm/aegis-realm.json
git commit -m "feat(keycloak): keycloak persistente com postgresql dedicado, realm aegis e export versionado"
```
