# 013 — AEGIS PMS BACKEND ARCHITECTURE BLUEPRINT
## Versão 1.0
## Arquitetura Oficial do Backend

---

# PROPÓSITO

Este documento define a arquitetura oficial do backend do Aegis PMS.

Seu objetivo é garantir:

- escalabilidade
- modularidade
- governança
- observabilidade
- segurança
- evolução contínua

O backend não deve ser construído como uma API comum.

Deve ser construído como uma plataforma.

---

# PRINCÍPIO ARQUITETURAL

O backend deve refletir os domínios do negócio.

Arquitetura guiada por domínio.

Não guiada por controllers.

Não guiada por tabelas.

Não guiada por frameworks.

---

# VISÃO DE ALTO NÍVEL

Aegis PMS

├ Core
├ Identity
├ Tenant
├ Product
├ Content
├ Asset
├ Form
├ Analytics
├ Knowledge
├ Audit
├ Platform
└ Infrastructure

---

# ESTILO ARQUITETURAL

Base:

Modular Monolith

---

Motivos:

- simplicidade operacional
- menor custo
- menor complexidade
- evolução segura

---

Evolução futura:

Modular Monolith
↓
Modular Services
↓
Microservices (se necessário)

---

# STACK PRINCIPAL

Java 25

Spring Boot

Spring Security

Spring Validation

Spring Data

PostgreSQL

Flyway

Docker

Keycloak

OpenTelemetry

Grafana

Prometheus

---

# ESTRUTURA DE PACOTES

br.com.aegis

├ application
├ domain
├ infrastructure
├ interfaces
└ shared

---

# ORGANIZAÇÃO POR MÓDULO

modules

├ identity
├ tenant
├ product
├ content
├ asset
├ form
├ analytics
├ knowledge
├ audit
└ platform

---

# ESTRUTURA DE MÓDULO

product

├ application
├ domain
├ infrastructure
├ interfaces
└ contracts

---

# CAMADA DOMAIN

Regra principal:

Não conhece framework.

---

Contém:

- entidades
- agregados
- value objects
- eventos
- regras de negócio

---

Exemplo:

Product

ProductStatus

ProductCreatedEvent

ProductAggregate

---

# CAMADA APPLICATION

Coordena casos de uso.

---

Exemplo:

CreateProductUseCase

ArchiveProductUseCase

CloneProductUseCase

---

Não contém regra de negócio.

---

# CAMADA INFRASTRUCTURE

Implementações técnicas.

---

Exemplos:

JPA

PostgreSQL

S3

Keycloak

Redis

Mensageria

---

# CAMADA INTERFACES

Exposição externa.

---

REST

GraphQL (futuro)

Webhooks

Eventos

---

# MULTI TENANCY

Regra fundamental:

Todo recurso pertence a um Tenant.

---

Estratégia inicial:

Shared Database

Shared Schema

TenantId obrigatório

---

Todas as tabelas:

tenant_id

---

# IDENTIDADE E SEGURANÇA

Responsável:

Keycloak

---

Domínio Identity

Gerencia:

Usuários

Perfis

Papéis

Permissões

Sessões

---

# MODELO DE AUTORIZAÇÃO

Role

↓

Permission

↓

Capability

---

Exemplo

CONTENT_CREATE

CONTENT_EDIT

CONTENT_DELETE

CONTENT_PUBLISH

---

# PRODUCT DOMAIN

Centro do sistema.

---

Produto é a entidade raiz.

Todos os demais módulos dependem dele.

---

# CONTENT DOMAIN

Responsável por:

Artigos

Páginas

Categorias

Tags

Workflow

Versionamento

---

Eventos

ContentCreated

ContentUpdated

ContentPublished

---

# ASSET DOMAIN

Biblioteca digital.

---

Tipos

Imagem

Vídeo

Áudio

Documento

---

Storage

S3 Compatible

---

# FORM DOMAIN

Responsável por:

Formulários

Campos

Respostas

Exportações

---

# ANALYTICS DOMAIN

Responsável por:

Métricas

KPIs

Dashboards

Relatórios

---

# KNOWLEDGE DOMAIN

Diferencial estratégico.

---

Responsável por:

Nós

Relações

Dependências

Conhecimento

---

Objetivo:

Criar um Knowledge Graph navegável.

---

# AUDIT DOMAIN

Todo evento relevante gera auditoria.

---

Registrar:

Quem

Quando

Onde

O que mudou

---

# EVENTOS DE DOMÍNIO

Estratégia:

Domain Events

---

Exemplos:

ProductCreated

ContentPublished

AssetUploaded

UserInvited

FormSubmitted

---

# EVENT BUS

Inicial:

In Memory

---

Futuro:

Kafka

RabbitMQ

Pulsar

---

# API DESIGN

Padrão:

REST

---

Exemplo:

/api/v1/products

/api/v1/content

/api/v1/assets

/api/v1/forms

---

# PADRÃO DE RESPOSTA

Success

Error

ValidationError

PaginatedResponse

---

# VALIDAÇÃO

Centralizada.

---

Bean Validation

---

Nunca:

Validação espalhada.

---

# OBSERVABILIDADE

Obrigatória.

---

Logs

Métricas

Tracing

Health Checks

---

Ferramentas:

OpenTelemetry

Prometheus

Grafana

---

# HEALTHCHECKS

Liveness

Readiness

Startup

---

# LOGGING

Estruturado.

JSON.

---

Campos obrigatórios

tenantId

productId

userId

traceId

timestamp

---

# BANCO DE DADOS

PostgreSQL

---

Estratégia

Flyway

Versionamento obrigatório.

---

Nunca alterar produção manualmente.

---

# CACHE

Redis

---

Uso:

Sessão

Permissões

Consultas

---

# FILE STORAGE

S3 Compatible

---

Abstração obrigatória.

Nunca depender de fornecedor específico.

---

# WEBHOOKS

Módulo próprio.

---

Eventos externos:

ContentPublished

FormSubmitted

AssetUploaded

---

# FEATURE FLAGS

Domínio Platform.

---

Permite:

ativar

desativar

experimentar

---

# SEGURANÇA

CSRF

Rate Limit

Headers

Auditoria

Permissões

---

Rate Limit

Bucket4j

---

# TESTES

Pirâmide:

Unitário

Integração

Arquitetura

E2E

---

Cobertura mínima

80%

---

Cobertura ideal

90%+

---

# CI/CD

Pipeline

Build

Test

Quality Gate

Docker

Deploy

---

Ferramentas

GitHub Actions

SonarQube

---

# ROADMAP ARQUITETURAL

Fase 1

Modular Monolith

---

Fase 2

Domain Events

---

Fase 3

Knowledge Graph

---

Fase 4

Observabilidade Completa

---

Fase 5

IA e Automações

---

# REGRA DE OURO

Nenhum módulo deve depender diretamente de outro módulo.

Comunicação preferencial:

Casos de uso

ou

Eventos

---

# CONCLUSÃO

O backend do Aegis PMS deve ser construído como uma plataforma modular.

Os domínios representam o negócio.

O framework é apenas uma ferramenta.

A arquitetura deve continuar compreensível quando existirem:

- dezenas de produtos
- centenas de usuários
- milhares de conteúdos
- milhões de eventos

Se uma decisão não sobreviver a esse cenário, ela deve ser revista.
