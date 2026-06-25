# Sprint 08 — Knowledge Graph MVP

## Identificação

- **Sprint:** 08 — Knowledge Graph MVP
- **Status:** ✅ Concluída
- **Resultado:** BUILD SUCCESS + Homologação manual concluída

## Resumo Executivo

A Sprint 08 entregou o MVP do Knowledge Graph do Aegis PMS utilizando modelo relacional em PostgreSQL, preparado para futura evolução sem comprometer os princípios Product First, Contract First, REST First e Modular Architecture.

Foram implementados o domínio do grafo, persistência, serviços, regras de consistência, API REST, controle de acesso por produto e módulo, além da validação completa por testes automatizados e testes manuais via Postman.

---

# Entregas

## Domínio

- GraphNode
- GraphEdge
- GraphNodeType
- GraphEdgeType

## Persistência

- Migration Flyway V3
- Repositories com Javadoc
- Índices e constraints
- JSONB para metadata
- Constraints de unicidade

## API

- POST /graph/nodes
- GET /graph/nodes
- GET /graph/nodes/{id}
- POST /graph/edges
- GET /neighbors
- GET /related

## Regras

- Mesmo tenant
- Mesmo produto
- Duplicidade bloqueada
- Nodes obrigatórios
- Module Gating
- Isolamento por produto

## Qualidade

- BUILD SUCCESS
- 253 testes
- 0 failures
- 0 errors
- JaCoCo aprovado
- Testes manuais via Postman aprovados

---

# Homologação Manual

Validado:

- Criação de nodes
- Criação de edges
- Busca por q
- Neighbor
- Related
- Duplicidade
- Produto sem módulo
- Produto fora do escopo
- Segurança JWT
- Persistência PostgreSQL

Todos os cenários aprovados.

---

# Principais descobertas da Sprint

## 1. Organização de pacotes

Foi identificado um comportamento recorrente do agente Codex de criar novas classes diretamente no pacote pai do módulo.

### Novo padrão do projeto

Toda classe nova deve ser criada em pacotes por responsabilidade:

- domain
- repository
- service
- controller
- contract
- dto
- mapper
- exception
- api
- module
- command

Nunca criar classes diretamente no pacote raiz do módulo.

---

## 2. Respeitar o módulo correto

O agente também moveu Product e Tenant para `core`.

Isso foi corrigido.

### Padrão definitivo

Product e Tenant são módulos de primeiro nível.

`core` deve conter apenas componentes compartilhados.

Nunca mover módulos de domínio para `core` sem autorização explícita.

---

## 3. Integração entre módulos

Foi encontrada violação de fronteira do Spring Modulith.

### Padrão definitivo

Nunca importar:

- entidades JPA
- repositories
- services internos

de outro módulo.

Toda comunicação entre módulos deve ocorrer através de APIs públicas (`product.api`) utilizando `@NamedInterface`.

---

## 4. Exception Handlers

Foi identificado acoplamento do CoreExceptionHandler com exceções específicas de módulos.

### Padrão definitivo

Cada módulo deve possuir seu próprio ExceptionHandler.

Core trata apenas exceções globais.

---

## 5. Auditoria Spring Modulith

Durante a sprint foi realizada varredura completa de dependências entre módulos.

Nova regra:

Sempre executar auditoria Modulith antes do encerramento da sprint.

---

## 6. Auditoria Sonar

A sprint evidenciou diversos pontos:

- imports mortos
- lambdas em assertThrows
- uso de relógio do sistema em testes
- construtores extensos
- validações redundantes

Nova regra:

Toda sprint deve encerrar com Sonar limpo ou apenas alertas conscientemente aceitos.

---

## 7. Testes

Fluxo obrigatório consolidado:

1. mvn clean verify
2. JaCoCo
3. Spring Modulith
4. Sonar
5. Postman
6. Validação manual do banco
7. Atualização do SPRINT-RESULTADO
8. Commit
9. GitFlow

---

# Observações para próximas sprints

- Manter comunicação entre módulos exclusivamente por APIs públicas.
- Revisar organização de pacotes antes do primeiro commit.
- Não mover classes entre módulos sem autorização.
- Validar arquitetura antes de implementar novas funcionalidades.
- Atualizar continuamente o SPRINT-RESULTADO.md.

---

# Resultado Final

A Sprint 08 consolidou a infraestrutura do Knowledge Graph do Aegis PMS, elevou o padrão arquitetural do projeto e definiu regras permanentes para organização modular, integração entre módulos e qualidade de código que deverão ser seguidas nas próximas sprints.
