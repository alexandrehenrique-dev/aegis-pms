# Sprint 02 — Identity and Access (Keycloak)

## Status

**SPRINT CONCLUÍDA COM SUCESSO** ✅

Data de conclusão: 21/06/2026

---

## Objetivo da Sprint

Implantar e validar a camada de Identity and Access Management do Aegis PMS utilizando Keycloak, seguindo os ADRs e a arquitetura definida pelo projeto.

---

## Entregas Realizadas

### Infraestrutura

- PostgreSQL dedicado para Keycloak
- Container Keycloak operacional
- Persistência via Docker Volume
- Integração entre Keycloak e PostgreSQL validada

### Realm

Realm criado:

```text
aegis
```

### Client

Client criado:

```text
aegis-web
```

Redirect URIs:

```text
http://localhost:8080/*
http://localhost:5173/*
```

### Modelo de Papéis

Realm Roles criadas:

```text
AEGIS_SUPER_ADMIN
AEGIS_TENANT_ADMIN
AEGIS_PRODUCT_MANAGER
AEGIS_EDITOR
AEGIS_VIEWER
```

### Usuário Administrativo

Usuário de validação criado e associado ao papel:

```text
AEGIS_SUPER_ADMIN
```

### OpenID Connect

Discovery endpoint validado:

```text
http://localhost:8282/realms/aegis/.well-known/openid-configuration
```

Retorno OIDC recebido com sucesso.

### Export do Realm

Artefato gerado:

```text
infra/keycloak/realm/aegis-realm.json
```

---

## Evidências de Validação

### Containers

```text
aegis-postgres
keycloak-postgres-aegis
aegis-keycloak
```

Todos operacionais.

### Banco de Dados

Validação executada:

```sql
SELECT count(*) FROM realm;
```

Resultado:

```text
2
```

Confirmando persistência dos realms.

### OpenID Discovery

Teste:

```bash
curl http://localhost:8282/realms/aegis/.well-known/openid-configuration
```

Resultado:

```text
HTTP 200 OK
```

---

## Estrutura Produzida

```text
infra/
└── keycloak/
    └── realm/
        └── aegis-realm.json
```

---

## Critérios de Aceite

| Critério | Status |
|-----------|---------|
| PostgreSQL Keycloak | ✅ |
| Keycloak operacional | ✅ |
| Realm aegis | ✅ |
| Client aegis-web | ✅ |
| Realm Roles | ✅ |
| Usuário administrativo | ✅ |
| Export do realm | ✅ |
| OIDC Discovery | ✅ |
| Persistência validada | ✅ |

---

## Resultado Final

A Sprint 02 foi concluída com sucesso.

O Aegis PMS possui agora uma camada de autenticação e autorização operacional baseada em Keycloak, com modelo canônico de papéis implementado, realm exportável e ambiente reproduzível via Docker Compose.

**Status final da Sprint:** APROVADA ✅
