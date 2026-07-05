# Keycloak

## Timeouts de sessão

O realm local em `realm/aegis-realm.json` usa valores confortáveis para desenvolvimento e demonstração:

- SSO Session Idle: 8 horas (`28800`)
- SSO Session Max: 24 horas (`86400`)
- Access Token Lifespan: 30 minutos (`1800`)

Produção deve usar valores mais restritos. O script `scripts/harden-realm-prod.sh` aplica:

- SSO Session Idle: 30 minutos (`1800`)
- SSO Session Max: 8 horas (`28800`)
- Access Token Lifespan: 5 minutos (`300`)

Para mudar por ambiente, ajuste o realm importado antes de recriar o container local, ou aplique os mesmos campos via `kcadm.sh update realms/aegis -s nomeDoCampo=valorEmSegundos`.
