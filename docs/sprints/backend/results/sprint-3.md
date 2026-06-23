# SPRINT-RESULTADO.md

## Etapa Em Execução

Etapa 03 — Keycloak persistente, realm/client/roles e export

Data: 2026-06-21

Status: ABERTA — aguardando execução/validação local e export real do realm

---

## Objetivo

Subir o Keycloak conectado ao PostgreSQL dedicado `keycloak-postgres`, configurar o realm `aegis`, o client `aegis-web`, as 5 roles globais canônicas `AEGIS_*` e exportar o realm para versionamento em `infra/keycloak/realm/aegis-realm.json`.

Regra central desta etapa:

Keycloak deve usar PostgreSQL dedicado e persistente. H2 não é permitido.

---

## Arquivos Criados ou Atualizados

Raiz:

* `docker-compose.yml`
* `.env`

Postman:

* `postman/aegis-postman-collection.json`

Keycloak:

* `infra/keycloak/realm/aegis-realm.json` — pendente até export real após configuração manual do realm

Documentação:

* `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md`

---

## Serviços Docker Confirmados/Previstos

Serviços existentes preservados:

* `aegis-postgres`
* `keycloak-postgres`

Serviço adicionado:

* `keycloak`

Container previsto:

* `aegis-keycloak`

Banco usado pelo Keycloak:

* host Docker: `keycloak-postgres`
* container real: `keycloak-postgres-aegis`
* database: `keycloak_aegis`
* user: `keycloak_aegis_user`

---

## Realm, Client e Roles

Realm obrigatório:

* `aegis`

Client obrigatório:

* `aegis-web`

Configuração do client:

* Tipo: OpenID Connect
* Client authentication: OFF
* Standard flow: ON
* Direct access grants: OFF, salvo teste temporário
* PKCE: S256, se disponível
* Valid redirect URIs:
  * `http://localhost:5173/*`
  * `http://localhost:8080/*`
* Web origins:
  * `http://localhost:5173`
  * `http://localhost:8080`

Roles globais obrigatórias:

* `AEGIS_SUPER_ADMIN`
* `AEGIS_TENANT_ADMIN`
* `AEGIS_PRODUCT_MANAGER`
* `AEGIS_EDITOR`
* `AEGIS_VIEWER`

Usuário de teste:

* Deve possuir e-mail verificado
* Deve possuir ao menos uma das roles globais acima

---

## Endpoints Confirmados

Endpoint OIDC de validação:

```txt
GET http://localhost:8282/realms/aegis/.well-known/openid-configuration
```

Resultado esperado:

```txt
issuer = http://localhost:8282/realms/aegis
```

Nenhum endpoint REST do backend Aegis foi criado nesta etapa.

---

## Contratos Criados

Nenhum contrato REST do backend foi criado nesta etapa.

Contrato operacional registrado na collection Postman cumulativa:

* pasta `03 - Keycloak persistente, realm, client, roles e export`
* validação OIDC do realm
* validação manual do banco do Keycloak
* validação manual de restart
* validação manual de export do realm

---

## Entidades Criadas

Nenhuma entidade de domínio Aegis criada nesta etapa.

---

## Casos de Uso Criados

Nenhum caso de uso backend criado nesta etapa.

Casos operacionais configurados/previstos:

* autenticar via Keycloak
* manter realm/client/users em PostgreSQL persistente
* exportar realm para versionamento

---

## Testes Previstos

Subida do Keycloak:

```bash
docker compose up -d keycloak
```

Validação OIDC:

```bash
curl http://localhost:8282/realms/aegis/.well-known/openid-configuration
```

Validação do banco:

```bash
docker exec -it keycloak-postgres-aegis psql -U keycloak_aegis_user -d keycloak_aegis -c "SELECT count(*) FROM realm;"
```

Restart:

```bash
docker compose restart keycloak
```

Persistência sem apagar volumes:

```bash
docker compose down
docker compose up -d
```

Export:

```bash
mkdir -p infra/keycloak/realm

docker exec -it aegis-keycloak /opt/keycloak/bin/kc.sh export \
  --dir /tmp/keycloak-export \
  --realm aegis \
  --users realm_file

docker cp aegis-keycloak:/tmp/keycloak-export/aegis-realm.json ./infra/keycloak/realm/aegis-realm.json
```

---

## Decisões Registradas

* O serviço Docker permanece chamado `keycloak-postgres`, pois esse é o hostname interno usado pelo Keycloak no `KC_DB_URL_HOST`.
* O container real do banco permanece `keycloak-postgres-aegis`, conforme etapa anterior, para evitar conflito com outros serviços no genesis-lab.
* A collection Postman oficial fica em `postman/aegis-postman-collection.json`.
* A variável `TZ` duplicada no `.env` foi normalizada no arquivo atualizado gerado.

---

## Pendências para Conclusão

* Aplicar `docker-compose.yml` atualizado no repositório.
* Subir `keycloak`.
* Criar realm/client/roles/usuário via console admin.
* Exportar `infra/keycloak/realm/aegis-realm.json`.
* Importar/substituir `postman/aegis-postman-collection.json` com a collection atualizada.
* Executar validações e registrar resultados finais.

---

## Critérios de Aceite

* Keycloak acessível em `http://localhost:8282`.
* Login admin funcional.
* Keycloak usa PostgreSQL dedicado, não H2.
* Realm `aegis` existe.
* Client `aegis-web` existe.
* As 5 roles globais `AEGIS_*` existem.
* Usuário de teste criado, e-mail verificado e role atribuída.
* Realm e usuário sobrevivem a restart e `docker compose down && docker compose up -d` sem `-v`.
* `infra/keycloak/realm/aegis-realm.json` existe e está versionado.
* Collection Postman cumulativa atualizada em `postman/aegis-postman-collection.json`.

---

Fim parcial da Etapa 03.
