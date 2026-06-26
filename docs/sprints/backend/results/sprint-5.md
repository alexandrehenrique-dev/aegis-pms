# Sprint 05 — Security Resource Server e /api/v1/me

# Sprint 05 — Identity and Access

## Status

✅ CONCLUÍDA

Data de conclusão: 23/06/2026

---

# Resumo Executivo

A Sprint 05 teve como objetivo estabelecer a fundação de autenticação e autorização do Aegis PMS utilizando Keycloak como Identity Provider oficial da plataforma.

Ao final da sprint foi entregue uma integração funcional baseada em OAuth2 Resource Server com validação de JWT, mapeamento de papéis do Keycloak para o modelo interno do sistema e endpoint autenticado para identificação do usuário logado.

Toda a implementação foi validada através de:

- testes unitários;
- testes MVC;
- testes de integração manual;
- validação real contra Keycloak;
- Sonar;
- JaCoCo.

---

# Objetivo da Sprint

Implementar:

- integração inicial com Keycloak;
- validação de JWT;
- conversão de roles;
- endpoint de identidade do usuário autenticado;
- infraestrutura de segurança base para as próximas sprints.

---

# Entregáveis Implementados

## Segurança

Implementados:

```txt
AuthenticatedUser
AuthenticatedUserProvider
JwtRoleConverter
SecurityConfig
```

Responsabilidades:

- leitura do JWT;
- extração de claims;
- extração de roles;
- conversão para modelo interno;
- configuração do Resource Server.

---

## API

Implementados:

```txt
MeController
MeResponse
MeResponseMapper
```

Endpoint:

```http
GET /api/v1/me
```

Resposta:

```json
{
  "subject": "aae4e8ea-2cee-4f3a-b261-c5083377bc57",
  "email": "loki@teste.com",
  "username": "loki",
  "name": "loki de asgard",
  "role": "super_admin"
}
```

---

# Validações Executadas

## Sem autenticação

Requisição:

```http
GET /api/v1/me
```

Resultado:

```txt
401 Unauthorized
```

Status:

✅ Aprovado

---

## JWT inválido

Resultado:

```txt
401 Unauthorized
```

Status:

✅ Aprovado

---

## JWT válido

Fluxo:

```txt
Keycloak
↓
OAuth2
↓
JWT
↓
Spring Security
↓
Resource Server
↓
Controller
```

Resultado:

```txt
200 OK
```

Status:

✅ Aprovado

---

# Collection Postman

Collection criada:

```txt
aegis-pms-sprint-05-identity-access.postman_collection.json
```

Contém:

```txt
Get Token
Me (401)
Me (Bearer Token)
```

---

# Realm Keycloak

Realm atualizado e exportado novamente.

Arquivo:

```txt
infra/keycloak/realm/aegis-realm.json
```

Alteração relevante:

```json
{
  "directAccessGrantsEnabled": true
}
```

Observação:

Esta configuração foi utilizada para validação manual utilizando:

```txt
grant_type=password
```

Não representa o fluxo oficial da SPA.

---

# Testes Implementados

## Unitários

```txt
AuthenticatedUserProviderTest
JwtRoleConverterTest
MeResponseMapperTest
```

---

## MVC

```txt
MeControllerTest
```

Cenários cobertos:

```txt
401 sem token
401 token inválido
200 token válido
```

---

## Cobertura Complementar

Necessária para atingir JaCoCo.

Implementados:

```txt
AegisApplicationTest
LocalStorageBootstrapTest
```

Motivação:

```txt
AegisApplication.main()
LocalStorageBootstrap
```

não estavam cobertos.

---

# Qualidade

## Sonar

Resultado:

```txt
Sem bugs
Sem vulnerabilidades
Sem code smells relevantes
```

Status:

✅ Aprovado

---

## JaCoCo

Resultado final:

```txt
100% aprovado
```

Status:

✅ Aprovado

---

# Problemas Encontrados Durante a Sprint

## 1. Base Package Incorreto

Foi identificado erro recorrente de geração de código.

Package correto do projeto:

```java
package br.com.byop.aegis;
```

Toda implementação futura deve respeitar este namespace.

---

## 2. Mudanças do Spring Boot 4

Exemplos encontrados para Boot 2 e Boot 3 não funcionaram integralmente.

Foi necessário adaptar:

```txt
@WebMvcTest
imports
test slices
```

para Spring Boot 4.1.

---

## 3. SecurityConfig em Testes MVC

Problema encontrado:

```txt
HttpSecurity bean não encontrado
```

Solução:

```java
@EnableWebSecurity
```

na configuração de segurança.

Observação:

Futuras implementações de testes MVC protegidos devem reutilizar este padrão.

---

## 4. Direct Access Grants

Erro encontrado:

```json
{
  "error": "unauthorized_client",
  "error_description": "Client not allowed for direct access grants"
}
```

Solução:

```txt
Direct Access Grants = ON
```

no client:

```txt
aegis-web
```

---

## 5. Cobertura JaCoCo

Problema encontrado:

```txt
AegisApplication.main()
LocalStorageBootstrap
```

sem cobertura.

Solução:

criação de testes específicos.

Observação:

classes bootstrap e classes de inicialização deverão possuir testes dedicados.

---

# Decisões Consolidadas

## Segurança

Padrão oficial:

```txt
Keycloak
OAuth2 Resource Server
JWT
```

Não criar autenticação própria.

---

## Papéis

Conversão oficial:

```txt
ROLE_SUPER_ADMIN
↓
super_admin
```

Responsabilidade:

```txt
JwtRoleConverter
```

---

## Endpoint de Identidade

Endpoint oficial:

```http
GET /api/v1/me
```

Deve permanecer como fonte oficial da identidade autenticada.

---

# Critérios de Aceite

## Segurança

- JWT validado
- Roles convertidas
- Endpoint protegido

Status:

✅ Aprovado

---

## API

- Contrato respeitado
- Endpoint funcional

Status:

✅ Aprovado

---

## Qualidade

- Sonar aprovado
- JaCoCo aprovado

Status:

✅ Aprovado

---

# Resultado Final

A Sprint 05 estabeleceu com sucesso a fundação de autenticação e autorização do Aegis PMS.

O sistema encontra-se integrado ao Keycloak, validando tokens JWT reais e expondo um endpoint autenticado de identidade.

Todos os critérios de aceite foram atendidos.

Não existem pendências técnicas abertas relacionadas à Sprint 05.

A Sprint encontra-se oficialmente encerrada.

---

# Observações para a Sprint 06

Antes de iniciar a próxima sprint:

1. Manter o package raiz:

```txt
br.com.byop.aegis
```

2. Utilizar sempre:

```txt
Keycloak
OAuth2 Resource Server
JWT
```

3. Não criar autenticação paralela.

4. Reutilizar:

```txt
AuthenticatedUserProvider
JwtRoleConverter
SecurityConfig
```

5. Reutilizar o endpoint:

```http
GET /api/v1/me
```

para validação de integração frontend.

6. Atualizar o realm exportado sempre que houver alteração estrutural no Keycloak.

7. Validar Sonar e JaCoCo antes de qualquer encerramento de sprint.
