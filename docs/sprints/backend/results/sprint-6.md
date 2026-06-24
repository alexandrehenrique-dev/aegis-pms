# Aegis PMS — Relatório Final da Sprint 06

**Sprint:** Sprint 06 — Auth Proxy, SMTP, Convite e Identity Access  
**Projeto:** Aegis PMS — Product Management System  
**Branch atual:** `develop`  
**Status final:** concluída e validada  
**Data de fechamento:** 2026-06-24

---

## 1. Resultado executivo

A Sprint 06 foi concluída com sucesso. O backend passou a ter um fluxo funcional de autenticação integrado ao Keycloak, com endpoints REST para login, refresh, logout, recuperação de senha, leitura do usuário autenticado e consulta administrativa de usuários.

Também foram validadas as configurações locais necessárias para o Keycloak, SMTP/MailHog, realm exportado, roles, usuário de teste, client web e client admin. Ao final, os testes automatizados passaram com sucesso e o gate de cobertura do JaCoCo foi atendido.

Validação final executada:

```bash
mvn clean verify
```

Resultado:

```text
Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

---

## 2. Escopo implementado

### 2.1 Auth proxy REST

Foram implementados endpoints de autenticação no backend, mantendo o backend como proxy REST para chamadas ao Keycloak.

Endpoints validados:

```http
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/forgot-password
```

Comportamentos validados:

- login com credenciais válidas retornando `accessToken`, `refreshToken`, `expiresIn` e `tokenType`;
- login com credenciais inválidas retornando erro controlado `INVALID_CREDENTIALS`;
- refresh token retornando novo par de tokens;
- logout retornando `204 No Content`;
- forgot password acionando fluxo de reset de senha via Keycloak Admin API;
- forgot password mantendo resposta genérica para evitar enumeração de usuários.

### 2.2 Endpoint `/me`

Foi validado o endpoint protegido:

```http
GET /api/v1/me
```

Resposta real validada com token JWT:

```json
{
  "subject": "aae4e8ea-2cee-4f3a-b261-c5083377bc57",
  "email": "loki@teste.com",
  "username": "loki",
  "name": "loki de asgard",
  "role": "super_admin"
}
```

Observação: o mapeamento de role Keycloak para role de domínio foi validado com a role `AEGIS_SUPER_ADMIN` resultando em `super_admin`.

### 2.3 Endpoints administrativos de usuários

Foram implementados e validados endpoints para leitura de usuários via Keycloak Admin API:

```http
GET /api/v1/users
GET /api/v1/users/{id}
```

Validação real executada:

```bash
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resposta validada com lista de usuários do realm:

```json
[
  {
    "id": "aae4e8ea-2cee-4f3a-b261-c5083377bc57",
    "username": "loki",
    "email": "alexandre.henrique.dev@gmail.com",
    "firstName": "loki",
    "lastName": "de asgard",
    "enabled": true
  },
  {
    "id": "fc713a07-2545-4591-afce-027b9251b596",
    "username": "teste",
    "email": "admin@test.local",
    "firstName": "Teste",
    "lastName": "da silva",
    "enabled": true
  }
]
```

Validação por ID:

```bash
curl http://localhost:8080/api/v1/users/fc713a07-2545-4591-afce-027b9251b596 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resposta validada:

```json
{
  "id": "fc713a07-2545-4591-afce-027b9251b596",
  "username": "teste",
  "email": "admin@test.local",
  "firstName": "Teste",
  "lastName": "da silva",
  "enabled": true
}
```

---

## 3. Keycloak

### 3.1 Realm

Realm utilizado:

```text
aegis
```

Export atualizado em:

```text
infra/keycloak/realm/aegis-realm.json
```

O export do realm foi atualizado após as configurações necessárias para autenticação, envio de e-mail e Admin API.

### 3.2 Clients

Foram utilizados/ajustados os clients necessários para:

- autenticação web;
- fluxo password grant usado pelo backend como proxy;
- chamadas administrativas via client credentials/Admin API.

### 3.3 Usuários e roles

Usuário de teste validado:

```text
username: loki
role: AEGIS_SUPER_ADMIN
```

A role `AEGIS_SUPER_ADMIN` foi reconhecida no JWT e convertida corretamente para o papel canônico de resposta `super_admin`.

### 3.4 SMTP / MailHog

Foi identificado erro no fluxo de forgot password:

```text
No sender address configured in the realm settings for emails
```

Causa: realm sem configuração de sender SMTP.

Correção realizada no Keycloak:

- configuração de SMTP no realm;
- configuração de sender/from address;
- integração local com MailHog;
- export atualizado do realm após validação.

Observação importante para próximas execuções: o fluxo de reset de senha depende da configuração SMTP estar presente no realm importado.

---

## 4. Configuração de ambiente

Foi corrigido problema de placeholders obrigatórios sem fallback no `application.yml`.

Erro encontrado:

```text
Not enough variable values available to expand 'KEYCLOAK_INTERNAL_BASE_URL'
```

Causa: propriedades Keycloak declaradas sem valores default:

```yaml
keycloak:
  issuer-uri: ${KEYCLOAK_ISSUER_URI}
  internal-base-url: ${KEYCLOAK_INTERNAL_BASE_URL}
  realm: ${KEYCLOAK_REALM}
  web-client-id: ${KEYCLOAK_WEB_CLIENT_ID}
```

Correção aplicada: defaults locais para execução em desenvolvimento.

Pontos importantes:

- `KEYCLOAK_ISSUER_URI` deve apontar para o issuer público usado na validação JWT;
- `KEYCLOAK_INTERNAL_BASE_URL` deve apontar para a URL usada pelo backend para se comunicar com Keycloak;
- em execução local fora do Docker, normalmente `http://localhost:8282`;
- dentro do Docker, o valor pode precisar ser o hostname do serviço Keycloak na rede compose.

---

## 5. Segurança

### 5.1 Resource Server

A aplicação segue configurada como OAuth2 Resource Server, validando JWT emitido pelo Keycloak.

Endpoints públicos:

```text
/actuator/health
/actuator/info
/v3/api-docs/**
/swagger-ui/**
/swagger-ui.html
/api/v1/auth/**
```

Endpoints protegidos:

```text
/api/v1/**
```

### 5.2 Conversão de roles

Foi validada a conversão de roles de realm access do Keycloak para authorities/roles internas.

Ponto validado:

```text
AEGIS_SUPER_ADMIN -> super_admin
```

### 5.3 CSRF

A configuração mantém CSRF desabilitado para API REST stateless.

Observação registrada: a decisão é coerente para API REST baseada em Bearer Token, mas deve continuar documentada e revisada quando houver frontend/browser com cookies ou sessões.

---

## 6. Testes automatizados

Foram adicionados e ajustados testes para:

- controller de autenticação;
- tratamento de exceções de autenticação;
- client de token do Keycloak;
- client admin do Keycloak;
- serviço de autenticação;
- controller de usuários;
- service de usuários;
- endpoint `/me`;
- provider de usuário autenticado;
- conversor de roles JWT;
- configuração de segurança;
- bootstrap de storage local.

Resultado final:

```text
Tests run: 70
Failures: 0
Errors: 0
Skipped: 0
```

### 6.1 JaCoCo

O gate de cobertura foi validado com:

```bash
mvn clean verify
```

Resultado:

```text
All coverage checks have been met.
BUILD SUCCESS
```

Durante a sprint, houve falhas intermediárias de cobertura, principalmente em branches do `KeycloakTokenClient` e `KeycloakAdminClient`. Foram adicionados testes para cobrir:

- login com sucesso;
- login inválido;
- conta desabilitada;
- body nulo/branco em erro 401;
- refresh com sucesso;
- refresh expirado;
- erro de servidor no refresh;
- logout com sucesso;
- erro de servidor no logout;
- admin token;
- busca de usuários;
- busca por ID;
- busca por e-mail;
- execução de reset password e cenários de erro.

---

## 7. Sonar / qualidade

Foram tratados pontos apontados pelo SonarQube for IDE:

- constantes extraídas para literais repetidos (`client_id`, `refresh_token`, grants etc.);
- remoção de duplicação em testes;
- uso de testes parametrizados onde fazia sentido;
- remoção de parâmetro não usado em `forgotPassword` ou tratamento adequado conforme implementação final;
- ajuste de lambdas em testes para evitar múltiplas invocações que poderiam lançar exceção;
- tratamento explícito/supressão consciente para CSRF em API REST stateless;
- substituição/isolamento de exceções genéricas onde aplicável.

Observação: a regra de credencial hard-coded foi considerada em contexto de teste/config local. Em arquivos de produção, valores sensíveis devem permanecer via variáveis de ambiente e `.env.example` deve conter apenas exemplos não sensíveis.

---

## 8. Correções e problemas encontrados durante a sprint

### 8.1 Base package correto

Observação crítica mantida para próximas sprints:

```text
br.com.byop.aegis
```

Não usar:

```text
br.com.aegis.pms
```

Esse ponto deve continuar sendo respeitado por agentes, Codex e próximas implementações.

### 8.2 `@PathVariable` e reflexão

Erro encontrado em teste de controller:

```text
Name for argument of type [java.lang.String] not specified, and parameter name information not available via reflection.
Ensure that the compiler uses the '-parameters' flag.
```

Correção recomendada/aplicada no controller:

```java
@PathVariable("id") String id
```

Observação para próximas sprints: declarar explicitamente nomes em `@PathVariable`, `@RequestParam` e bindings semelhantes para evitar dependência de metadata de compilação.

### 8.3 `urlContaining` indisponível

Durante os testes com WireMock, foi identificado que o método `urlContaining` não estava disponível no contexto/import utilizado.

Solução: substituir por alternativas compatíveis com a versão usada, como `urlPathEqualTo` combinado com query params, ou `urlEqualTo` quando a URL completa for previsível.

### 8.4 Configuração local vs Docker

Foi identificado que algumas URLs precisam diferenciar execução local e execução via Docker Compose.

Pontos sensíveis:

- backend local acessando Keycloak por `localhost:8282`;
- backend em container acessando Keycloak pelo nome do serviço Docker;
- issuer JWT precisa bater exatamente com o `iss` do token emitido.

### 8.5 SMTP obrigatório para forgot password

O Keycloak exige configuração de e-mail no realm para executar `execute-actions-email`.

Sem SMTP/sender configurado, o endpoint falha com 500 no Keycloak:

```text
Failed to send execute actions email: No sender address configured in the realm settings for emails
```

O realm exportado deve preservar essa configuração para ambientes locais.

### 8.6 Logs de teste

O teste de `AuthExceptionHandlerTest` gera log de erro proposital para validar o handler de exceção:

```text
Keycloak authentication error
```

Esse log não representa falha real do teste. O teste passa e valida resposta controlada.

### 8.7 Mockito / Java Agent

Durante os testes aparece warning do Mockito/ByteBuddy:

```text
Mockito is currently self-attaching to enable the inline-mock-maker.
Dynamic loading of agents will be disallowed by default in a future release.
```

Não bloqueia a sprint. Observação para manutenção futura: avaliar configuração explícita do Mockito como agent no Maven caso o JDK futuro passe a bloquear dynamic agent loading por padrão.

### 8.8 Encoding Maven

O Maven exibiu warnings de platform encoding:

```text
File encoding has not been set, using platform encoding UTF-8
```

Não bloqueia a sprint. Recomendação para próxima melhoria de qualidade: declarar encoding no `pom.xml`, se ainda não estiver consolidado:

```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
```

---

## 9. Arquivos alterados / adicionados observados no fechamento

Com base no `git status`, havia alterações em:

```text
.env.example
backend/pom.xml
backend/src/main/java/br/com/byop/aegis/AegisApplication.java
backend/src/main/java/br/com/byop/aegis/security/SecurityConfig.java
backend/src/main/resources/application.yml
backend/src/test/java/br/com/byop/aegis/api/me/MeControllerTest.java
backend/src/test/java/br/com/byop/aegis/shared/storage/LocalStorageBootstrapTest.java
docker-compose.yml
docs/sprints/README.md
docs/sprints/backend/00_indice_e_instrucoes.md
docs/sprints/backend/05_security_resource_server_e_me.md
docs/sprints/frontend/06_keycloak_login_ui_custom.md
infra/keycloak/realm/aegis-realm.json
```

Novos caminhos relevantes:

```text
backend/src/main/java/br/com/byop/aegis/identity/
backend/src/test/java/br/com/byop/aegis/identity/
backend/src/test/java/br/com/byop/aegis/security/SecurityConfigTest.java
infra/keycloak/themes/
docs/sprints/backend/06_auth_proxy_smtp_e_convite.md
```

Também houve renumeração dos documentos posteriores da trilha backend, deslocando as sprints antigas `06..25` para `07..26`, pois a Sprint 06 passou a representar Auth Proxy, SMTP e Convite.

---

## 10. Validações manuais realizadas

### 10.1 Login válido

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "loki",
    "password": "123456"
  }'
```

Resultado: token retornado com sucesso.

### 10.2 Login inválido

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "loki",
    "password": "SENHA_DO_USUARIO"
  }'
```

Resultado:

```json
{"code":"INVALID_CREDENTIALS"}
```

### 10.3 `/me`

```bash
curl http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resultado: dados do usuário autenticado retornados com role canônica.

### 10.4 Refresh

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

Resultado: novo token retornado.

### 10.5 Logout

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

Resultado:

```text
HTTP/1.1 204
```

### 10.6 Forgot password

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "loki@teste.com"
  }'
```

Resultado final: fluxo validado após configuração SMTP/sender no Keycloak e export do realm.

### 10.7 Usuários

```bash
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resultado: lista de usuários retornada.

```bash
curl http://localhost:8080/api/v1/users/{id} \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resultado: usuário por ID retornado.

---

## 11. Observações para próximas sprints

1. Manter o base package `br.com.byop.aegis` em todo código novo.
2. Não avançar domínio sem respeitar a hierarquia documental: Constituição > ADR > Blueprint > Sprint.
3. Continuar usando abordagem Product First, Contract First e REST First.
4. Garantir que novos endpoints nasçam com testes de controller, service/client e validação de segurança.
5. Para endpoints com path variables, declarar explicitamente o nome do parâmetro.
6. Antes de validar forgot password em ambiente novo, confirmar SMTP/sender do Keycloak.
7. Ao alterar realm, sempre exportar novamente para `infra/keycloak/realm/aegis-realm.json`.
8. Separar claramente URLs internas Docker e URLs locais.
9. Manter `mvn clean verify` como gate final, não apenas `mvn clean test`.
10. Observar warnings do Mockito/ByteBuddy em JDKs futuros.
11. Avaliar padronização de encoding Maven para eliminar warnings recorrentes.
12. Avaliar se a cobertura 100% continuará sendo regra global ou se haverá exceções justificadas por pacote/classe em fases futuras.
13. Não commitar tokens, senhas reais, secrets ou exports contendo credenciais sensíveis sem revisão.
14. Validar o conteúdo de `infra/keycloak/themes/` antes do commit para garantir que apenas tema necessário foi incluído.
15. Revisar renumeração dos documentos de sprint no commit para evitar quebra de links internos.

---

## 12. Estado final

A sprint está pronta para fechamento técnico.

Critérios atendidos:

- backend compila;
- testes passam;
- JaCoCo passa;
- autenticação integrada ao Keycloak;
- refresh/logout funcionais;
- `/me` protegido funcional;
- usuários via Admin API funcionais;
- forgot password funcional após SMTP;
- realm exportado;
- documentação de sprint ajustada;
- observações críticas registradas.

Comando final validado:

```bash
mvn clean verify
```

Resultado final:

```text
BUILD SUCCESS
```

