# 014 — AEGIS PMS DATA & PERSISTENCE BLUEPRINT
## Versão 1.0
## Arquitetura de Dados e Persistência

---

# PROPÓSITO

Este documento define a estratégia oficial de persistência do Aegis PMS.

Seu objetivo é garantir:

- consistência
- rastreabilidade
- escalabilidade
- auditabilidade
- evolução segura

Este documento representa a fundação permanente da plataforma.

---

# PRINCÍPIO FUNDAMENTAL

O banco existe para servir aos domínios.

Nunca o contrário.

Modelagem baseada em:

- negócio
- agregados
- eventos
- auditoria

Não baseada apenas em CRUD.

---

# TECNOLOGIA PRINCIPAL

Banco Principal

PostgreSQL

---

Migrações

Flyway

---

Cache

Redis

---

Assets

S3 Compatible Storage

---

Knowledge Graph

Inicialmente PostgreSQL

Evolução futura:

Neo4j opcional

---

# ESTRATÉGIA MULTI TENANT

Modelo Inicial

Shared Database

Shared Schema

---

Toda tabela possui:

tenant_id

---

Benefícios

- simplicidade
- menor custo
- governança centralizada

---

# HIERARQUIA DE DADOS

Tenant

└ Product

    ├ Content

    ├ Asset

    ├ Form

    ├ Analytics

    ├ Knowledge

    └ Audit

---

# TABELA TENANT

tenant

Campos:

id

name

slug

status

created_at

updated_at

---

# TABELA PRODUCT

product

Campos:

id

tenant_id

name

slug

description

status

created_at

updated_at

---

# REGRAS

Produto sempre pertence a um Tenant.

---

# CONTENT DOMAIN

## content

Representa a entidade principal.

Campos:

id

tenant_id

product_id

type

title

slug

status

current_version_id

created_by

created_at

updated_at

---

## content_version

Versionamento completo.

Campos:

id

content_id

version_number

body

metadata_json

created_by

created_at

---

Objetivo:

Nunca perder histórico.

---

## content_category

Categorias.

---

## content_tag

Tags.

---

## content_workflow

Fluxo editorial.

Campos:

content_id

status

changed_by

changed_at

---

# ASSET DOMAIN

## asset

Campos:

id

tenant_id

product_id

filename

mime_type

storage_path

size

checksum

status

created_at

---

## asset_version

Versionamento.

---

## asset_tag

Relacionamento.

---

# FORM DOMAIN

## form

Campos:

id

tenant_id

product_id

name

slug

status

created_at

---

## form_field

Campos dinâmicos.

---

## form_submission

Respostas.

---

## form_submission_value

Valores individuais.

---

# ANALYTICS DOMAIN

## metric

Métricas coletadas.

---

Campos:

id

tenant_id

product_id

metric_name

metric_value

period

recorded_at

---

## kpi

Indicadores estratégicos.

---

## dashboard_snapshot

Materialização de dashboards.

---

# KNOWLEDGE GRAPH DOMAIN

## knowledge_node

Elemento principal.

Campos:

id

tenant_id

product_id

node_type

reference_id

title

created_at

---

Tipos:

CONTENT

ASSET

FORM

CATEGORY

TAG

USER

PRODUCT

---

## knowledge_relationship

Representa conexões.

Campos:

id

source_node_id

target_node_id

relationship_type

created_at

---

Exemplos

RELATED_TO

DEPENDS_ON

AUTHORED_BY

BELONGS_TO

REFERENCES

---

# USER DOMAIN

## user_profile

Informações complementares.

---

Usuários ficam no Keycloak.

---

Banco mantém:

Preferências

Metadados

Relacionamentos

---

# AUDIT DOMAIN

## audit_event

Tabela mais importante.

---

Campos:

id

tenant_id

product_id

entity_type

entity_id

action

performed_by

timestamp

payload_json

---

Exemplos

CONTENT_PUBLISHED

ASSET_UPLOADED

USER_INVITED

PERMISSION_GRANTED

---

# EVENT STORE

Versão inicial:

Audit + Domain Events

---

## domain_event

Campos:

id

event_type

aggregate_type

aggregate_id

payload_json

occurred_at

processed

---

Objetivo

Base para automações futuras.

---

# CONFIGURAÇÕES

## feature_flag

Campos:

id

tenant_id

product_id

flag_key

enabled

---

## integration

Integrações.

---

## webhook

Webhooks.

---

# SOFT DELETE

Obrigatório.

---

Campos:

deleted

deleted_at

deleted_by

---

Nunca remover dados críticos fisicamente.

---

# VERSIONAMENTO

Obrigatório para:

Content

Asset

Form

Knowledge

---

Objetivo

Histórico completo.

---

# INDEXAÇÃO

Índices obrigatórios

tenant_id

product_id

slug

status

created_at

updated_at

---

# BUSCA

Fase 1

PostgreSQL Full Text Search

---

Fase 2

OpenSearch

---

Fase 3

Busca Semântica IA

---

# CACHE

Redis

---

Objetos:

Permissões

Sessões

Dashboards

Feature Flags

Consultas pesadas

---

# STORAGE

Abstração obrigatória.

---

Interface:

StorageProvider

---

Implementações:

S3

MinIO

Cloudflare R2

---

# MIGRAÇÕES

Flyway

---

Estratégia

V1__initial_schema.sql

V2__content_module.sql

V3__asset_module.sql

...

---

Nunca alterar migração executada.

---

# OBSERVABILIDADE DOS DADOS

Registrar:

tempo de consulta

volume

falhas

locks

deadlocks

---

# RETENÇÃO

Auditoria

Indefinida

---

Conteúdo

Indefinida

---

Eventos

Configurável

---

Analytics

Configurável

---

# EVOLUÇÃO FUTURA

V1

PostgreSQL

---

V2

Knowledge Graph avançado

---

V3

OpenSearch

---

V4

Neo4j opcional

---

V5

Data Lake

---

# MATRIZ DE CRITICIDADE

Crítico

Tenant

Product

Content

Audit

Permission

---

Importante

Asset

Form

Knowledge

---

Derivado

Analytics

Snapshots

Cache

---

# REGRA DE OURO

Todo dado do Aegis deve responder:

Quem criou?

Quando criou?

Quem alterou?

Quando alterou?

A qual Tenant pertence?

A qual Produto pertence?

Se uma entidade não responder essas perguntas,
sua modelagem está incompleta.

---

# CONCLUSÃO

A persistência do Aegis PMS não é apenas armazenamento.

Ela é a memória institucional da plataforma.

Tudo deve ser rastreável.

Tudo deve ser versionável.

Tudo deve ser auditável.

Tudo deve evoluir sem perda de contexto.
