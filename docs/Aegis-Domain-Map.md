# Aegis Domain Map

Este documento identifica os domínios de negócio do Aegis a partir de `docs/Aegis-Vision.md` e dos artefatos em `docs/*`.

O Aegis é uma plataforma administrativa multi-produto, multi-tenant, modular e orientada a contratos. Sua raiz operacional é o Produto. Páginas, assets, formulários, SEO, analytics e módulos específicos existem para administrar produtos digitais, não para transformar o Aegis em um CMS page-first.

## Critérios de Classificação

CORE DOMAIN:

- Domínios que expressam a vantagem central do Aegis.
- Domínios que protegem Product First, Contract First, CMS First, modularidade e tenant isolation.
- Domínios sem os quais o produto deixa de ser Aegis e vira um painel/CMS genérico.

SUPPORTING DOMAIN:

- Domínios importantes para entregar a operação de negócio, mas que suportam o core.
- Domínios que podem evoluir por módulo, produto, fase ou vertical.
- Domínios que carregam regras próprias, mas não definem sozinhos a identidade estratégica do Aegis.

GENERIC DOMAIN:

- Capacidades necessárias, reutilizáveis e com baixo diferencial estratégico.
- Domínios normalmente resolvidos por tecnologia, provider, plataforma ou governança operacional.
- Domínios que devem ser mantidos coerentes com o negócio, mas não personalizados além do necessário.

## CORE DOMAIN

### 1. Gestão de Produtos Digitais

Classificação: CORE DOMAIN.

Responsabilidade:

- Administrar a unidade digital central do Aegis.
- Definir identidade, categoria, status, lifecycle, owners, configurações, conteúdo associado, capacidades habilitadas e contrato público de cada produto.
- Garantir que todos os recursos editáveis existam dentro de um produto pertencente a um tenant.

Entidades principais:

- Produto.
- Categoria de Produto.
- Configuração de Produto.
- Owner de Produto.
- Status de Produto.

Aggregate roots:

- Produto.

Value objects:

- ProductId.
- ProductSlug.
- ProductName.
- ProductCategory.
- ProductStatus.
- DefaultLocale.
- PublicKey.
- ProductSettings.
- ProductMetadata.

Eventos de domínio:

- ProdutoCriado.
- ProdutoAtivado.
- ProdutoSuspenso.
- ProdutoArquivado.
- ProdutoRemovidoLogicamente.
- ProdutoReativado.
- CategoriaDeProdutoDefinida.
- ConfiguracaoDeProdutoAlterada.
- ContratoPublicoDoProdutoDisponibilizado.
- ContratoPublicoDoProdutoCongelado.

Dependências:

- Depende de Tenant para pertencimento.
- Depende de Membership para autorização.
- Depende de Feature Management para capacidades reais.
- Depende de Contratos JSON para exposição pública.
- Depende de Auditoria para rastreabilidade de mudanças.
- Depende de Revisões quando conteúdo publicado é afetado.

Bounded contexts:

- Product Management Context.
- Tenant Context.
- Feature Governance Context.
- Public Contract Context.

APIs expostas:

- API administrativa de produtos.
- API administrativa de lifecycle do produto.
- API pública de contrato do produto.
- API pública de descoberta do produto por slug/domínio/chave pública.

APIs consumidas:

- API de tenants e contexto ativo.
- API de memberships e permissões.
- API de feature catalog e product features.
- API de auditoria.
- API de contratos JSON.

### 2. Tenancy, Memberships e Autorização Contextual

Classificação: CORE DOMAIN.

Responsabilidade:

- Definir a fronteira primária de pertencimento, autorização, isolamento, configuração e operação.
- Resolver onde um usuário pode atuar e o que ele pode fazer naquele tenant e naquele produto.
- Impedir vazamento cross-tenant e escalation horizontal.

Entidades principais:

- Tenant.
- Tenant Settings.
- Tenant Membership.
- Product Membership.
- Tenant Invitation.
- Role de Tenant.
- Role de Produto.
- Current Tenant.
- User Local.

Aggregate roots:

- Tenant.
- TenantMembership.
- TenantInvitation.

Value objects:

- TenantId.
- TenantSlug.
- TenantStatus.
- TenantType.
- MembershipStatus.
- TenantRole.
- ProductRole.
- InvitationTokenHash.
- InvitationExpiry.
- CurrentTenantContext.
- PermissionDecision.

Eventos de domínio:

- TenantCriado.
- TenantAtivado.
- TenantSuspenso.
- TenantArquivado.
- TenantSettingsAlterados.
- MembershipConvidada.
- MembershipAtivada.
- MembershipSuspensa.
- MembershipRemovida.
- RoleDeTenantAlterada.
- RoleDeProdutoAlterada.
- UltimoOwnerBloqueadoParaRemocao.
- ConviteCriado.
- ConviteAceito.
- ConviteRevogado.
- ConviteExpirado.
- ContextoAtivoSelecionado.
- AcessoCrossTenantNegado.

Dependências:

- Depende de Identity para autenticação do usuário.
- Depende de Produto para autorização granular.
- Depende de Features para validar capacidade ativa.
- Depende de Auditoria para registrar ações sensíveis.
- Depende de Segurança e LGPD para políticas de isolamento e dados pessoais.

Bounded contexts:

- Tenant Context.
- Access Context.
- Invitation Context.
- Identity Integration Context.

APIs expostas:

- API de contexto autenticado.
- API administrativa de tenants.
- API administrativa de memberships.
- API administrativa de convites.
- API de aceite de convite.
- API de troca/seleção de tenant ativo.

APIs consumidas:

- Keycloak/OIDC para identidade.
- API de produtos para validar pertencimento.
- API de features para validar capacidade.
- API de auditoria.
- API de notificações para envio de convites.

### 3. Feature Governance e Modularidade de Produto

Classificação: CORE DOMAIN.

Responsabilidade:

- Governar quais capacidades existem na plataforma e quais estão habilitadas em cada produto.
- Controlar menus, endpoints, contratos, limites, permissões, dependências e futuro billing metadata por produto.
- Manter o Aegis modular sem expor módulos indiscriminadamente.

Entidades principais:

- Feature Catalog Item.
- Product Feature.
- Feature Dependency.
- Feature Permission.
- Product Feature Permission.
- Feature Limit.
- Feature Configuration.

Aggregate roots:

- FeatureCatalogItem.
- ProductFeature.

Value objects:

- FeatureKey.
- FeatureStatus.
- FeatureType.
- ModuleKey.
- DependencyKind.
- FeatureLimitSet.
- FeatureConfig.
- BillingMetadata.
- MenuImpact.
- PermissionRule.

Eventos de domínio:

- FeatureCatalogada.
- FeaturePublicada.
- FeatureDepreciada.
- FeatureRetirada.
- FeatureHabilitadaParaProduto.
- FeatureDesabilitadaParaProduto.
- FeatureSuspensaParaProduto.
- DependenciaDeFeatureBloqueouAtivacao.
- LimiteDeFeatureAlterado.
- PermissaoDeFeatureAlterada.
- ContratoDeProdutoAtualizadoPorFeature.

Dependências:

- Depende de Produto como alvo de habilitação.
- Depende de Tenant para isolamento.
- Depende de Membership/Autorização para permissões efetivas.
- Depende de Contratos JSON para refletir capacidades ativas.
- Depende de Navegação e UX administrativa para menus dinâmicos.
- Depende de Auditoria para alterações de feature.

Bounded contexts:

- Feature Governance Context.
- Product Management Context.
- Module Registry Context.
- Contract Assembly Context.

APIs expostas:

- API administrativa de catálogo de features.
- API administrativa de features do produto.
- API administrativa de enable/disable/config/limits.
- API de consulta de capacidades efetivas para montagem de menu e contrato.

APIs consumidas:

- API de produtos.
- API de tenants e memberships.
- API de contratos.
- API de auditoria.
- API de navegação administrativa.

### 4. Content Platform: Pages, Sections, Blocks e Content Types

Classificação: CORE DOMAIN.

Responsabilidade:

- Permitir que produtos digitais tenham conteúdo estruturado, editável, versionável, publicável e exposto por contratos.
- Modelar páginas, seções, blocos e entradas estruturadas sem acoplar frontends à estrutura interna do banco.
- Suportar domínios verticais por Content Types versionados e validados por schema.

Entidades principais:

- Page.
- Section.
- Block.
- Block Type.
- Content Type Definition.
- Content Entry.
- Field Definition.
- Resource Relation.

Aggregate roots:

- Page.
- ContentTypeDefinition.
- ContentEntry.

Value objects:

- PageSlug.
- Locale.
- EditorialStatus.
- SectionLayout.
- BlockType.
- BlockData.
- ContentTypeKey.
- FieldSchema.
- ContentEntryData.
- ResourceReference.

Eventos de domínio:

- PaginaCriada.
- PaginaAlterada.
- PaginaEnviadaParaRevisao.
- PaginaAprovada.
- PaginaPublicada.
- PaginaArquivada.
- SecaoAdicionada.
- SecoesReordenadas.
- BlocoAdicionado.
- BlocoAtualizado.
- BlocosReordenados.
- ContentTypeRegistrado.
- ContentTypeVersionado.
- ContentEntryCriada.
- ContentEntryPublicada.
- RelacaoDeConteudoCriada.

Dependências:

- Depende de Produto e Tenant.
- Depende de Feature ativa para edição e exposição.
- Depende de Revisões para versionamento editorial.
- Depende de Contratos JSON para publicação.
- Depende de Assets para mídias referenciadas.
- Depende de SEO, i18n, Navegação, Busca e Analytics.
- Depende de Auditoria para ações editoriais.

Bounded contexts:

- Content Authoring Context.
- Structured Content Context.
- Editorial Workflow Context.
- Public Content Delivery Context.

APIs expostas:

- API administrativa de páginas, seções e blocos.
- API administrativa de content types e entries.
- API pública de páginas publicadas.
- API pública de content entries publicadas.
- API de schema de block/content type.

APIs consumidas:

- API de produto e features.
- API de assets.
- API de revisões.
- API de SEO.
- API de i18n.
- API de navegação.
- API de busca/indexação.
- API de analytics.
- API de auditoria.

### 5. Contratos JSON Canônicos e Public Delivery

Classificação: CORE DOMAIN.

Responsabilidade:

- Ser a fronteira estável entre Aegis e frontends externos.
- Expor produtos, páginas, assets, formulários, navegação, busca, analytics e demais recursos por contratos versionados.
- Proteger consumidores contra mudanças internas do banco e do domínio.

Entidades principais:

- Contract.
- Contract Registry.
- Contract Version.
- Contract Schema.
- Contract Example.
- Contract Owner.
- Public Contract Assembly.
- Error Contract.
- Pagination Contract.
- Metadata Contract.

Aggregate roots:

- ContractDefinition.
- ContractRegistry.

Value objects:

- ContractKey.
- ContractVersion.
- JsonSchema.
- ContractStatus.
- ContractOwner.
- ContractEnvelope.
- LinkRelation.
- ErrorCode.

Eventos de domínio:

- ContratoRegistrado.
- ContratoAtivado.
- ContratoDepreciado.
- ContratoRetirado.
- BreakingChangeDetectado.
- NovaVersaoMajorDeContratoPublicada.
- ContratoValidadoEmRuntime.
- ContratoInvalidoDetectado.

Dependências:

- Depende de todos os domínios que expõem dados públicos ou administrativos.
- Depende de Feature Governance para listar apenas capacidades ativas.
- Depende de Produto e Tenant para escopo.
- Depende de Revisões para entregar somente conteúdo publicado.
- Depende de API REST para transporte no MVP.
- Depende de Governança para versionamento e depreciação.

Bounded contexts:

- Contract Governance Context.
- Public API Context.
- Admin API Context.
- Consumer Compatibility Context.

APIs expostas:

- API pública de contratos de produto.
- APIs públicas de recursos publicados.
- API administrativa de contratos e registry.
- Contratos de erro, paginação, metadata e coleção.

APIs consumidas:

- APIs de produto, conteúdo, assets, forms, SEO, navegação, busca e analytics.
- API de feature governance.
- API de revisão/publicação.
- API de auditoria e observabilidade para falhas de contrato.

### 6. Workflow Editorial, Revisões e Publicação

Classificação: CORE DOMAIN.

Responsabilidade:

- Governar o ciclo editorial de conteúdos e configurações publicáveis.
- Preservar histórico por snapshots, permitir publicação, rollback e comparação.
- Separar estado atual, revisão e publicação.

Entidades principais:

- Revision.
- Publication Pointer.
- Editorial Resource.
- Revision Snapshot.
- Editorial Status.
- Diff Result.

Aggregate roots:

- RevisionedResource.
- Revision.

Value objects:

- RevisionId.
- RevisionNumber.
- ResourceType.
- ResourceId.
- Snapshot.
- PublicationStatus.
- PublishedAt.
- Diff.

Eventos de domínio:

- RevisaoCriada.
- RevisaoEnviadaParaReview.
- RevisaoAprovada.
- RevisaoPublicada.
- RevisaoArquivada.
- RollbackExecutado.
- ComparacaoDeRevisoesSolicitada.
- PublicacaoBloqueadaPorContratoIncompativel.
- TraducaoMarcadaComoStale.

Dependências:

- Depende de Conteúdo, SEO, Navegação, Formulários, Assets e Traduções como recursos revisionáveis.
- Depende de Produto e Tenant.
- Depende de Membership para permissão editorial.
- Depende de Contratos JSON para validar exposição pública.
- Depende de Auditoria para rastrear quem publicou ou reverteu.
- Depende de Cache e Busca para invalidar projeções públicas.

Bounded contexts:

- Editorial Workflow Context.
- Versioning Context.
- Publication Context.

APIs expostas:

- API administrativa de revisões por recurso.
- API administrativa de publicação.
- API administrativa de rollback.
- API administrativa de diff.

APIs consumidas:

- APIs dos recursos revisionáveis.
- API de autorização.
- API de contratos.
- API de auditoria.
- API de cache/invalidação.
- API de busca/indexação.

## SUPPORTING DOMAIN

### 7. Asset Management

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Gerenciar mídias, arquivos, documentos, currículos, downloads, capas e anexos usados pelos produtos.
- Garantir storage tenant-scoped, metadados, acessibilidade, versionamento, sensibilidade LGPD e entrega pública controlada.

Entidades principais:

- Asset.
- Asset Variant.
- Asset Metadata.
- Asset Relation.
- Asset Version.
- Storage Object.

Aggregate roots:

- Asset.

Value objects:

- AssetId.
- AssetType.
- StorageKey.
- MimeType.
- FileSize.
- Checksum.
- AltText.
- AssetStatus.
- SensitiveFlag.
- VariantSet.

Eventos de domínio:

- AssetUploaded.
- AssetValidated.
- AssetProcessingRequested.
- AssetReady.
- AssetMetadataUpdated.
- AssetVariantGenerated.
- AssetReferenced.
- AssetArchived.
- AssetDeletedLogically.
- SensitiveAssetMarked.

Dependências:

- Depende de Produto, Tenant, Feature e Membership.
- Depende de Jobs para processamento de imagens.
- Depende de LGPD para assets sensíveis.
- Depende de SEO e Conteúdo para metadados e referências.
- Depende de Contratos JSON para entrega pública.
- Depende de Auditoria para upload/delete.

Bounded contexts:

- Asset Context.
- Storage Context.
- Media Processing Context.

APIs expostas:

- API administrativa de upload.
- API administrativa de listagem e metadados.
- API administrativa de soft delete.
- API pública de asset publicado.

APIs consumidas:

- API de produto/features.
- Storage provider.
- API de jobs.
- API de auditoria.
- API de LGPD/retention.

### 8. Forms, Submissions e Leads

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Definir formulários por produto e processar submissões de visitantes.
- Validar schema, consentimento, anexos, workflow, retenção e notificações.
- Suportar contatos, orçamentos, candidaturas, newsletter, sugestões, bugs e demais entradas externas.

Entidades principais:

- Form Definition.
- Form Field.
- Form Submission.
- Consent Record.
- Submission Attachment.
- Form Workflow.

Aggregate roots:

- FormDefinition.
- FormSubmission.

Value objects:

- FormKey.
- FormSchema.
- FieldValidation.
- SubmissionStatus.
- ConsentText.
- ConsentPolicyVersion.
- SourceIpHash.
- RetentionDays.

Eventos de domínio:

- FormularioCriado.
- FormularioPublicado.
- FormularioDesativado.
- SubmissaoRecebida.
- SubmissaoRejeitadaPorValidacao.
- ConsentimentoRegistrado.
- SubmissaoEnviadaParaReview.
- SubmissaoTratada.
- SubmissaoAnonimizada.
- NotificacaoDeSubmissaoSolicitada.

Dependências:

- Depende de Produto, Tenant, Feature e Contratos Públicos.
- Depende de LGPD para consentimento, retenção e anonimização.
- Depende de Assets para anexos.
- Depende de Notificações para alertas.
- Depende de Analytics para eventos de submit.
- Depende de Auditoria para exportações e dados sensíveis.

Bounded contexts:

- Form Builder Context.
- Submission Intake Context.
- Lead Processing Context.

APIs expostas:

- API administrativa de forms.
- API administrativa de submissions.
- API pública de submissão de formulário.
- API administrativa de exportação autorizada.

APIs consumidas:

- API de produto/features.
- API de assets.
- API de notificações.
- API de LGPD.
- API de analytics.
- API de auditoria.

### 9. SEO e Descoberta Pública

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Gerenciar metadados de descoberta pública dos produtos.
- Controlar title, description, canonical, robots, Open Graph, schema.org, redirects, slugs, hreflang e sitemap.

Entidades principais:

- SEO Metadata.
- SEO Redirect.
- Sitemap.
- Canonical URL.
- Hreflang Set.

Aggregate roots:

- SeoMetadata.
- SeoRedirect.

Value objects:

- SeoTitle.
- SeoDescription.
- CanonicalUrl.
- RobotsDirective.
- OpenGraphData.
- SchemaOrgData.
- RedirectCode.
- PublicPath.

Eventos de domínio:

- SeoMetadataCriado.
- SeoMetadataAlterado.
- RedirectCriado.
- RedirectAlterado.
- SitemapGerado.
- HreflangAtualizado.
- SlugPublicoAlterado.

Dependências:

- Depende de Produto, Conteúdo, i18n e Navegação.
- Depende de Revisões para versionamento.
- Depende de Contratos para exposição pública.
- Depende de Cache para invalidação.
- Depende de Auditoria para alterações.

Bounded contexts:

- SEO Context.
- Public Discovery Context.

APIs expostas:

- API administrativa de SEO por recurso.
- API administrativa de redirects.
- API pública de SEO no contrato de página.
- API pública de sitemap.

APIs consumidas:

- API de conteúdo publicado.
- API de i18n.
- API de navegação.
- API de auditoria.
- API de cache.

### 10. Navegação e Estrutura de Informação

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Modelar menus públicos e administrativos.
- Expor navegação por produto, locale, feature ativa, role e status de publicação.
- Garantir descoberta coerente sem hardcode por frontend.

Entidades principais:

- Navigation.
- Navigation Item.
- Menu Type.
- Target Reference.
- Breadcrumb.

Aggregate roots:

- Navigation.

Value objects:

- NavigationType.
- NavigationItemLabel.
- NavigationTargetType.
- NavigationTargetRef.
- ItemPosition.
- RequiredFeature.
- NavigationStatus.

Eventos de domínio:

- NavegacaoCriada.
- ItemDeNavegacaoAdicionado.
- ItemDeNavegacaoReordenado.
- ItemDeNavegacaoRemovido.
- NavegacaoPublicada.
- MenuAdministrativoMontado.
- ItemOcultadoPorFeatureOuPermissao.

Dependências:

- Depende de Produto, Tenant, Feature e Membership.
- Depende de Conteúdo para alvos publicados.
- Depende de i18n para menus por locale.
- Depende de Revisões e Contratos para publicação.
- Depende de Analytics para medir descoberta.

Bounded contexts:

- Navigation Context.
- Information Architecture Context.
- Admin Menu Context.

APIs expostas:

- API administrativa de navegação.
- API pública de navegação.
- API interna de menu administrativo por tenant/role/features.

APIs consumidas:

- API de conteúdo.
- API de feature governance.
- API de autorização.
- API de i18n.
- API de contratos.

### 11. Internacionalização e Traduções

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Gerenciar conteúdo multilíngue por produto.
- Definir locale padrão, traduções, fallback, status STALE, URL strategy e hreflang.

Entidades principais:

- Translation Entry.
- Translation Registry.
- Locale Configuration.
- Translatable Resource.

Aggregate roots:

- TranslationEntry.

Value objects:

- Locale.
- FieldPath.
- TranslationStatus.
- SourceVersion.
- FallbackLocale.
- HreflangMap.

Eventos de domínio:

- TraducaoCriada.
- TraducaoPublicada.
- TraducaoMarcadaComoStale.
- FallbackAplicado.
- LocaleAdicionadoAoProduto.
- LocaleRemovidoDoProduto.
- HreflangAtualizado.

Dependências:

- Depende de Produto e suas configurações de locale.
- Depende de Conteúdo, SEO, Navegação, Forms e Assets como recursos traduzíveis.
- Depende de Revisões para versionamento.
- Depende de Feature/Limits para idiomas suportados.
- Depende de Busca para indexação por locale.

Bounded contexts:

- I18n Context.
- Translation Workflow Context.

APIs expostas:

- API administrativa de traduções.
- API pública de recursos por locale.
- API de metadata de fallback e hreflang.

APIs consumidas:

- APIs dos recursos traduzíveis.
- API de revisão/publicação.
- API de SEO.
- API de busca.
- API de contratos.

### 12. Busca, Descoberta e Recuperação

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Indexar e recuperar conteúdo por tenant, produto, locale, status e visibilidade.
- Suportar busca pública e administrativa respeitando publicação e permissões.

Entidades principais:

- Search Document.
- Search Query.
- Search Result.
- Search Index.
- Search Visibility.

Aggregate roots:

- SearchDocument.

Value objects:

- SearchDocumentId.
- SearchScope.
- SearchVisibility.
- SearchStatus.
- SearchScore.
- SearchSnippet.
- QueryText.

Eventos de domínio:

- DocumentoDeBuscaIndexado.
- DocumentoDeBuscaAtualizado.
- DocumentoDeBuscaRemovido.
- BuscaPublicaExecutada.
- BuscaAdministrativaExecutada.
- MigracaoParaMotorDedicadoSinalizada.

Dependências:

- Depende de Conteúdo publicado, i18n, SEO, Navegação e Produto.
- Depende de Jobs para indexação assíncrona.
- Depende de Membership para busca administrativa.
- Depende de Analytics para registrar buscas.
- Depende de PostgreSQL FTS no MVP.

Bounded contexts:

- Search Context.
- Discovery Context.

APIs expostas:

- API pública de busca.
- API administrativa de busca.
- API interna de indexação.

APIs consumidas:

- API de conteúdo e publicação.
- API de autorização.
- API de jobs.
- API de analytics.
- PostgreSQL FTS.

### 13. Analytics e Métricas de Produto

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Coletar eventos de uso por tenant e produto.
- Produzir agregações e dashboards iniciais respeitando privacidade, retenção e isolamento.

Entidades principais:

- Analytics Event.
- Analytics Daily Rollup.
- Dashboard Metric.
- Session Hash.

Aggregate roots:

- AnalyticsEvent.
- AnalyticsRollup.

Value objects:

- EventType.
- ResourceType.
- ResourceId.
- ReferrerClass.
- SessionHash.
- OccurredAt.
- RetentionPolicy.
- MetricCount.

Eventos de domínio:

- EventoAnaliticoRecebido.
- EventoAnaliticoEnfileirado.
- EventoAnaliticoIngerido.
- RollupDiarioGerado.
- DashboardAtualizado.
- EventoExpiradoPorRetencao.

Dependências:

- Depende de Produto e Tenant.
- Depende de LGPD para pseudonimização e retenção.
- Depende de Jobs para ingestão e rollup.
- Depende de Cache para dashboard.
- Depende de Conteúdo, Forms, Busca e Assets como fontes de eventos.

Bounded contexts:

- Analytics Context.
- Product Insights Context.

APIs expostas:

- API pública de ingestão de eventos.
- API administrativa de dashboards.
- API administrativa de rollups e métricas.

APIs consumidas:

- API de produto/tenant.
- API de jobs.
- API de LGPD/retention.
- API de cache.

### 14. Notificações e Comunicação

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Transformar eventos de domínio em comunicações para usuários, equipes e canais operacionais.
- Gerenciar registry de eventos, templates, preferências, entregas, retry e dead-letter.

Entidades principais:

- Notification Event.
- Notification Delivery.
- Notification Template.
- Notification Preference.
- Channel Dispatcher.

Aggregate roots:

- NotificationEvent.
- NotificationDelivery.

Value objects:

- EventKey.
- NotificationPayload.
- Channel.
- Recipient.
- TemplateKey.
- DeliveryStatus.
- RetryPolicy.

Eventos de domínio:

- NotificationEventCreated.
- NotificationDeliveryQueued.
- NotificationSent.
- NotificationFailed.
- NotificationRetryScheduled.
- NotificationDeadLettered.
- NotificationPreferenceChanged.

Dependências:

- Depende de eventos de Forms, Memberships, Conteúdo, Comentários, Sugestões, Jobs e Operação.
- Depende de Tenant para preferências e customização.
- Depende de Jobs para envio assíncrono.
- Depende de Integrações para Telegram/e-mail/in-app.
- Depende de LGPD para evitar vazamento de dados sensíveis.
- Depende de Auditoria para falhas finais e eventos sensíveis.

Bounded contexts:

- Notification Context.
- Communication Context.

APIs expostas:

- API administrativa de preferências.
- API administrativa de templates.
- API interna de criação de notification events.
- API interna de deliveries.

APIs consumidas:

- API de jobs.
- Providers de e-mail, Telegram e in-app.
- API de tenants e usuários.
- API de auditoria.

### 15. Colaboração, Comentários e Sugestões

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Permitir colaboração sobre recursos do ecossistema, especialmente WikiDev e Loki.
- Modelar comentários, threads, moderação, sugestões, ideias, votos e backlog colaborativo.

Entidades principais:

- Comment.
- Comment Thread.
- Suggestion.
- Vote.
- Moderation Decision.
- Resource Reference.

Aggregate roots:

- Comment.
- Suggestion.

Value objects:

- CommentStatus.
- SuggestionStatus.
- SuggestionCategory.
- AuthorLabel.
- VoteCount.
- ModerationMode.
- ResourceReference.

Eventos de domínio:

- ComentarioCriado.
- ComentarioAprovado.
- ComentarioRejeitado.
- ComentarioPublicado.
- ComentarioArquivado.
- SugestaoCriada.
- SugestaoTriada.
- SugestaoPlanejada.
- SugestaoConcluida.
- VotoRegistrado.
- DenunciaRecebida.

Dependências:

- Depende de Produto, Tenant e Feature.
- Depende de Conteúdo ou recurso comentável.
- Depende de Membership quando autoria autenticada é exigida.
- Depende de Notificações para menções/moderação.
- Depende de Auditoria e LGPD.
- Depende de Busca para descoberta pós-MVP.

Bounded contexts:

- Collaboration Context.
- Moderation Context.
- Suggestion Backlog Context.

APIs expostas:

- API pública/administrativa de comentários conforme feature.
- API administrativa de moderação.
- API pública/administrativa de sugestões.
- API de votação.

APIs consumidas:

- API de conteúdo/recurso referenciado.
- API de autorização.
- API de notificações.
- API de auditoria.
- API de LGPD.

### 16. Domínios Verticais de Produto

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Representar capacidades específicas de produtos reais do ecossistema BYOP.
- Usar o core de produto, conteúdo, features, contratos, assets, forms e analytics para entregar módulos especializados.

Entidades principais:

- Institucional/CMSS: evento, galeria, timeline, contato, página institucional.
- Música/Maestro Beton: serviço, agenda, repertório, vídeo, depoimento, orçamento.
- RH/Conecta Talentos: vaga, candidatura, banco de talentos, lead corporativo, perfil de empresa.
- Portfólio/Alexandre Dev: projeto, case, experiência, skill, artigo, download.
- Biblioteca/Loki: manifesto, poema, reflexão, trecho, livro, playlist, referência musical.
- Wiki/WikiDev: categoria, tópico, artigo, relação, bug report, contributor application.

Aggregate roots:

- Event.
- Service.
- JobPosting.
- CandidateSubmission.
- Project.
- Manifesto.
- KnowledgeArticle.
- KnowledgeTopic.
- QuoteRequest.
- Lead.

Value objects:

- EventDate.
- Venue.
- SkillSet.
- SalaryRange.
- ApplicationStatus.
- AnonymousAuthorFlag.
- KnowledgeRelation.
- ServiceCategory.
- PortfolioRole.
- PlaylistReference.

Eventos de domínio:

- EventoPublicado.
- OrcamentoSolicitado.
- VagaPublicada.
- CandidaturaRecebida.
- ProjetoPublicado.
- ManifestoPublicado.
- ArtigoTecnicoPublicado.
- RelacaoDeConhecimentoCriada.
- BugReportRecebido.
- LeadCorporativoRecebido.

Dependências:

- Dependem de Produto, Tenant, Feature, Conteúdo e Content Types.
- Dependem de Forms para leads, candidaturas, orçamentos, bug reports e sugestões.
- Dependem de Assets para galerias, vídeos, documentos, currículos e downloads.
- Dependem de SEO, Busca, Analytics, Revisões e Contratos.
- Dependem de LGPD quando lidam com dados pessoais ou sensíveis.

Bounded contexts:

- Institutional Module Context.
- Music Module Context.
- HR Module Context.
- Portfolio Module Context.
- Library Module Context.
- Wiki Module Context.

APIs expostas:

- APIs administrativas dos content types verticais.
- APIs públicas de eventos, vagas, artigos, projetos, biblioteca e wiki.
- APIs públicas de formulários verticais.

APIs consumidas:

- APIs do core de produto/tenant/feature.
- API de conteúdo.
- API de forms.
- API de assets.
- API de contratos.
- API de SEO, busca e analytics.

### 17. LGPD, Privacidade e Governança de Dados

Classificação: SUPPORTING DOMAIN.

Responsabilidade:

- Definir finalidade, consentimento, retenção, exportação, eliminação, anonimização e classificação de dados.
- Bloquear ou condicionar features quando a conformidade de dados pessoais não estiver atendida.

Entidades principais:

- Data Subject Request.
- Consent Record.
- Retention Policy.
- Data Classification.
- Anonymization Task.
- Data Map.

Aggregate roots:

- DataSubjectRequest.
- RetentionPolicy.

Value objects:

- DataClass.
- ConsentTimestamp.
- RetentionDays.
- SubjectEmail.
- AnonymizationReason.
- Purpose.

Eventos de domínio:

- ConsentimentoColetado.
- ConsentimentoAusenteBloqueouSubmissao.
- ExportacaoDeDadosSolicitada.
- EliminacaoDeDadosSolicitada.
- DadosAnonimizados.
- RetencaoExpirada.
- FeatureBloqueadaPorLGPD.
- MapaDeDadosGerado.

Dependências:

- Depende de Tenants para políticas por tenant.
- Depende de Forms, Assets, Memberships, Comentários, Sugestões e Analytics.
- Depende de Jobs para retenção e anonimização.
- Depende de Auditoria para registrar solicitações e exportações.
- Depende de Segurança para acesso forte e criptografia.

Bounded contexts:

- Privacy Context.
- Data Governance Context.
- Retention Context.

APIs expostas:

- API administrativa de exportação LGPD.
- API administrativa de eliminação/anonimização.
- API administrativa de data map.
- API de configuração de retenção por tenant.

APIs consumidas:

- APIs dos domínios que armazenam PII.
- API de jobs.
- API de auditoria.
- API de segurança/autorização.

## GENERIC DOMAIN

### 18. Identity Provider Integration

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Integrar o Aegis ao Keycloak para autenticação, sessão, tokens, usuários globais e roles globais.
- Sincronizar usuário local no primeiro login válido.

Entidades principais:

- Identity Provider User.
- Local User.
- Auth Session.
- Global Role.
- Token Claims.

Aggregate roots:

- LocalUser.

Value objects:

- UserId.
- IdentityProviderSubject.
- Email.
- DisplayName.
- EmailVerified.
- GlobalRole.
- JwtClaims.

Eventos de domínio:

- LoginRealizado.
- LoginFalhou.
- UsuarioLocalCriado.
- UsuarioLocalSincronizado.
- SessaoEncerrada.
- UsuarioDesativadoNoIdP.

Dependências:

- Depende de Keycloak.
- Suporta Tenant/Membership para autorização posterior.
- Depende de Auditoria para login e falhas.
- Depende de LGPD para anonimização local.

Bounded contexts:

- Identity Integration Context.
- Authentication Context.

APIs expostas:

- API de auth context.
- API de callback/integração OIDC conforme configuração.
- API de logout/sessão conforme necessidade.

APIs consumidas:

- Keycloak OIDC/JWT/JWKS.
- API de memberships.
- API de auditoria.
- API de LGPD.

### 19. API REST e OpenAPI

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Definir o transporte oficial do MVP.
- Padronizar versionamento, namespaces, status codes, erros, paginação e documentação OpenAPI.

Entidades principais:

- API Version.
- Endpoint.
- Error Contract.
- Pagination Contract.
- OpenAPI Specification.
- Route Namespace.

Aggregate roots:

- ApiSpecification.

Value objects:

- ApiVersion.
- RoutePath.
- HttpStatus.
- ErrorCode.
- PaginationRequest.
- PaginationMetadata.

Eventos de domínio:

- EndpointPublicado.
- EndpointDepreciado.
- BreakingChangeDeApiDetectado.
- NovaVersaoDeApiCriada.
- OpenApiAtualizada.

Dependências:

- Depende de Contratos JSON.
- Expõe todos os contextos via REST.
- Depende de Segurança para autenticação/autorização.
- Depende de Governança para breaking changes.

Bounded contexts:

- REST API Context.
- API Governance Context.

APIs expostas:

- `/api/v1/public`.
- `/api/v1/auth`.
- `/api/v1/admin`.
- `/api/v1/internal`.
- OpenAPI versionada.

APIs consumidas:

- Serviços de aplicação dos domínios.
- Contract registry.
- Permission evaluator.
- Observabilidade e auditoria.

### 20. Persistência, Banco e Migrations

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Prover persistência relacional com PostgreSQL, shared schema, tenantId obrigatório, JSONB controlado e migrations Flyway.
- Garantir evolução de dados com compatibilidade histórica.

Entidades principais:

- Database Schema.
- Migration.
- Index.
- Constraint.
- JSONB Payload.

Aggregate roots:

- MigrationPlan.

Value objects:

- MigrationVersion.
- TableName.
- IndexDefinition.
- ConstraintDefinition.
- TenantScopedKey.
- JsonbSchemaUsage.

Eventos de domínio:

- MigrationAplicada.
- MigrationFalhou.
- SchemaExpandido.
- BackfillConcluido.
- FormaAntigaContraida.
- IndiceCriado.

Dependências:

- Suporta todos os domínios persistentes.
- Depende de ADR-0004 e ADR-0010.
- Depende de Deploy para aplicação de migrations.
- Depende de Backup/DR para continuidade.

Bounded contexts:

- Persistence Context.
- Schema Evolution Context.

APIs expostas:

- Não expõe API de negócio pública.
- Exposição interna via repositories tenant-scoped.
- Health/readiness de banco.

APIs consumidas:

- PostgreSQL.
- Flyway.
- Observabilidade.
- Deploy pipeline.

### 21. Jobs e Processamento Assíncrono

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Executar processamento assíncrono, idempotente, tenant-aware e rastreável.
- Suportar imagens, notificações, busca, analytics, retenção, anonimização e cache warming.

Entidades principais:

- Job.
- Job Queue.
- Dead Letter.
- Retry Attempt.
- Idempotency Key.

Aggregate roots:

- Job.

Value objects:

- JobType.
- JobPayload.
- JobStatus.
- ScheduledAt.
- Attempts.
- MaxAttempts.
- CorrelationId.
- IdempotencyKey.

Eventos de domínio:

- JobEnfileirado.
- JobIniciado.
- JobConcluido.
- JobFalhou.
- JobReagendado.
- JobMovidoParaDeadLetter.
- LoteDeTenantIsolado.

Dependências:

- Depende de Tenant para jobs tenant-owned.
- Suporta Assets, Notificações, Busca, Analytics, LGPD e Cache.
- Depende de Observabilidade para rastreio.
- Depende de Auditoria em falhas sensíveis.

Bounded contexts:

- Async Processing Context.
- Job Orchestration Context.

APIs expostas:

- API interna de enqueue.
- API interna/administrativa de status de jobs.
- Health de fila.

APIs consumidas:

- APIs dos domínios solicitantes.
- Banco de jobs.
- Observabilidade.
- Auditoria.

### 22. Cache, Performance e Distribuição

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Melhorar latência e distribuição de contratos públicos, navegação, features e dashboards sem quebrar tenant isolation.
- Gerenciar chaves tenant-scoped e invalidação por eventos.

Entidades principais:

- Cache Entry.
- Cache Key.
- Evict Event.
- Contract Cache.
- Dashboard Cache.

Aggregate roots:

- CachePolicy.

Value objects:

- CacheKey.
- Ttl.
- SurrogateKey.
- CacheScope.
- EvictionReason.

Eventos de domínio:

- CachePreenchido.
- CacheInvalidado.
- ContractVersionBumped.
- CacheWarmSolicitado.
- RedisNecessarioSinalizado.
- CdnPurgeSolicitado.

Dependências:

- Depende de Produto, Tenant e Contract Version.
- Suporta Contratos, Conteúdo, Navegação, Features e Analytics.
- Depende de Jobs para warming.
- Depende de Observabilidade para métricas.

Bounded contexts:

- Cache Context.
- Performance Context.
- Public Delivery Optimization Context.

APIs expostas:

- API interna de cache get/set/evict.
- API interna de cache warming.

APIs consumidas:

- Caffeine no MVP.
- Redis futuro.
- CDN futura.
- Eventos de publicação, feature, SEO, navegação e rollup.

### 23. Observabilidade

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Fornecer logs, métricas, traces, correlation IDs, alertas, dashboards e retenção operacional.
- Permitir rastreabilidade ponta a ponta entre request, domínio, job, notificação e auditoria.

Entidades principais:

- Log Event.
- Metric.
- Trace.
- Alert.
- Correlation Context.
- SLO.

Aggregate roots:

- ObservabilitySignal.

Value objects:

- CorrelationId.
- TraceId.
- LogLevel.
- MetricName.
- SloTarget.
- AlertStatus.

Eventos de domínio:

- CorrelationIdGerado.
- MetricaRegistrada.
- LogEstruturadoEmitido.
- TracePropagado.
- AlertaDisparado.
- DeadLetterAlertado.
- SloViolado.

Dependências:

- Suporta todos os domínios.
- Depende de Jobs e Notificações para dead-letter.
- Integra com Auditoria por correlation/trace id.
- Depende de Segurança para não vazar PII/segredos.

Bounded contexts:

- Observability Context.
- Operations Monitoring Context.

APIs expostas:

- Health checks internos.
- Métricas internas.
- Dashboards e alertas operacionais.

APIs consumidas:

- Logs estruturados.
- Metrics backend futuro.
- Tracing backend futuro.
- Alerting backend futuro.

### 24. Auditoria e Rastreabilidade

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Registrar ações relevantes, imutáveis e rastreáveis: quem fez, quando, em qual tenant/produto, sobre qual recurso e com qual resultado.
- Sustentar segurança, compliance, suporte, investigação e governança.

Entidades principais:

- Audit Log.
- Audit Event.
- Actor.
- Resource Reference.
- Audit Result.

Aggregate roots:

- AuditEvent.

Value objects:

- AuditEventId.
- ActorId.
- ActorType.
- AuditAction.
- AuditResult.
- ResourceType.
- Origin.
- CorrelationId.

Eventos de domínio:

- AuditEventRecorded.
- AcaoSensivelAuditada.
- AcessoNegadoAuditado.
- ExportacaoAuditada.
- SuperAdminActionAuditada.
- DeadLetterAuditada.

Dependências:

- Consome eventos de todos os domínios relevantes.
- Depende de Tenant/Produto quando há contexto.
- Depende de Observabilidade para correlation/trace.
- Depende de LGPD para retenção e anonimização sem PII indevida.

Bounded contexts:

- Audit Context.
- Traceability Context.

APIs expostas:

- API administrativa de consulta de audit logs.
- API interna de registro de auditoria.

APIs consumidas:

- Eventos de domínio.
- Observabilidade.
- Membership/autorização para consulta.

### 25. Segurança Operacional e Modelo de Ameaças

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Definir controles contra spoofing, tampering, repudiation, information disclosure, denial of service e elevation of privilege.
- Proteger autenticação, autorização, uploads, secrets, rate limiting, headers e tenant isolation.

Entidades principais:

- Security Policy.
- Threat Model.
- Secret.
- Rate Limit Rule.
- Upload Policy.
- Security Incident.

Aggregate roots:

- SecurityPolicy.
- SecurityIncident.

Value objects:

- ThreatCategory.
- SecretKey.
- EncryptedSecret.
- RateLimitKey.
- SecurityHeaderSet.
- IncidentSeverity.

Eventos de domínio:

- TenantSpoofingNegado.
- AcessoCrossTenantDetectado.
- SegredoRotacionado.
- RateLimitAtingido.
- UploadBloqueado.
- IncidenteDeSegurancaDeclarado.
- ADRDeSegurancaNecessario.

Dependências:

- Depende de Identity, Tenant/Membership e Auditoria.
- Protege todas as APIs administrativas e públicas sensíveis.
- Depende de Observabilidade para alertas.
- Depende de Governança para decisões estratégicas.

Bounded contexts:

- Security Context.
- Threat Modeling Context.
- Secret Management Context.

APIs expostas:

- API interna de permission evaluation.
- API interna de secret access/rotation.
- API de health/security checks conforme operação.

APIs consumidas:

- Keycloak.
- Audit log.
- Observabilidade.
- Storage e integration providers.

### 26. Deploy, Ambientes e Entrega

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Controlar build, release, deploy, promotion, rollback, health checks e migrations por ambiente.
- Entregar backend e SPA administrativa na mesma origem no MVP.

Entidades principais:

- Environment.
- Release.
- Deploy.
- Build Artifact.
- Health Check.
- Rollback Plan.

Aggregate roots:

- Release.
- Deployment.

Value objects:

- EnvironmentName.
- ReleaseVersion.
- ArtifactId.
- DeploymentStatus.
- HealthStatus.
- PromotionGate.

Eventos de domínio:

- BuildGerado.
- ReleaseCriado.
- DeployIniciado.
- MigrationExecutadaNoDeploy.
- HealthCheckAprovado.
- DeployPromovido.
- RollbackExecutado.
- DeployFalhou.

Dependências:

- Depende de Banco/Migrations.
- Depende de Observabilidade e Health Checks.
- Depende de Segurança para secrets e configuração por ambiente.
- Depende de ADR-0009 para entrega da SPA.

Bounded contexts:

- Delivery Context.
- Environment Management Context.

APIs expostas:

- Health liveness/readiness.
- Endpoints internos de operação.

APIs consumidas:

- CI/CD.
- Docker/Docker Compose.
- Flyway.
- Banco.
- Observabilidade.

### 27. Backup e Disaster Recovery

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Garantir continuidade operacional por backup, retenção, restore testado, RPO, RTO e runbooks de desastre.
- Preservar tenant isolation e dados sensíveis em cópias e restores.

Entidades principais:

- Backup.
- Restore Test.
- Recovery Point.
- Disaster Recovery Runbook.
- RPO/RTO Target.

Aggregate roots:

- BackupPolicy.
- DisasterRecoveryIncident.

Value objects:

- BackupSchedule.
- RetentionWindow.
- RecoveryPointObjective.
- RecoveryTimeObjective.
- RestoreStatus.

Eventos de domínio:

- BackupExecutado.
- BackupFalhou.
- RestoreTestado.
- DisasterRecoveryDeclarado.
- AmbienteDeRecuperacaoProvisionado.
- RestoreConcluido.
- PostMortemSolicitado.

Dependências:

- Depende de PostgreSQL, Keycloak, storage de assets e secrets.
- Depende de Observabilidade para detectar incidentes.
- Depende de Governança para revisão pós-incidente.
- Depende de Segurança e LGPD para dados sensíveis.

Bounded contexts:

- Continuity Context.
- Disaster Recovery Context.

APIs expostas:

- Runbooks operacionais.
- Status interno de backup/restore quando implementado.

APIs consumidas:

- Banco PostgreSQL.
- Keycloak export/backup.
- Storage de assets.
- Cofre de segredos.
- Observabilidade.

### 28. Governança Arquitetural, ADRs e IA

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Preservar decisões estruturais, contratos, módulos, features, segurança, dados, deploy e operação.
- Impedir arquitetura paralela e orientar humanos/agentes de IA a partir da constituição do produto.

Entidades principais:

- ADR.
- Architectural Decision.
- Decision Status.
- Governance Policy.
- Deprecation Plan.
- AI Governance Checklist.

Aggregate roots:

- ArchitectureDecisionRecord.
- GovernancePolicy.

Value objects:

- AdrNumber.
- AdrStatus.
- DecisionImpact.
- DeprecationWindow.
- ReviewCadence.

Eventos de domínio:

- ADRProposto.
- ADRAceito.
- ADRSubstituido.
- ADRDepreciado.
- RevisaoArquiteturalSolicitada.
- BreakingChangeGovernado.
- AgenteDeIAValidouConstituicao.

Dependências:

- Depende da documentação fonte de verdade.
- Governa todos os domínios quando há decisão HIGH, STRATEGIC ou IRREVERSIBLE.
- Depende de Auditoria e Observabilidade em incidentes críticos.

Bounded contexts:

- Architecture Governance Context.
- ADR Context.
- AI Governance Context.

APIs expostas:

- Não expõe API de negócio no MVP.
- Expõe políticas, documentos, ADRs e processos de revisão.

APIs consumidas:

- Documentação mestre.
- ADRs.
- Contratos.
- APIs e módulos documentados.

### 29. Eirene: Experiência Administrativa

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Prover a SPA administrativa por onde usuários operam tenants, produtos, features, conteúdo, assets, forms, SEO, analytics e auditoria.
- Montar experiência dinâmica por tenant ativo, produto, role e features habilitadas.

Entidades principais:

- Admin Screen.
- Admin Menu.
- Tenant Selector.
- Product Dashboard.
- Editor View.
- Design System Component.

Aggregate roots:

- AdminWorkspace.

Value objects:

- ActiveTenantView.
- ActiveProductView.
- MenuItemVisibility.
- ScreenState.
- UiPermissionHint.

Eventos de domínio:

- TenantSelecionadoNaUI.
- ProdutoSelecionadoNaUI.
- MenuMontadoPorFeature.
- AcaoOcultadaPorPermissao.
- EditorAberto.
- PreviewEstruturalGerado.

Dependências:

- Depende de Identity, Auth Context, Tenant, Produto, Features e Contratos Administrativos.
- Depende de todos os domínios operáveis pelo painel.
- Depende de Deploy para ser servida pelo Spring Boot.
- Segurança real permanece no backend.

Bounded contexts:

- Admin Experience Context.
- Workspace Context.

APIs expostas:

- Não expõe API de negócio própria no MVP.
- Entrega SPA administrativa na mesma origem.

APIs consumidas:

- API de auth/context.
- API administrativa de todos os domínios.
- API de contratos administrativos.

### 30. SaaS Futuro e Billing Reservado

Classificação: GENERIC DOMAIN.

Responsabilidade:

- Reservar espaço conceitual para planos, assinaturas, ciclos de cobrança, invoices e payment providers.
- Não executar cobrança no MVP e não bloquear permissões por inadimplência no MVP.

Entidades principais:

- Plan.
- Subscription.
- Plan Feature.
- Billing Cycle.
- Invoice.
- Payment Provider.
- Product Purchase.

Aggregate roots:

- Subscription.
- Plan.
- Invoice.

Value objects:

- PlanKey.
- Price.
- Currency.
- BillingPeriod.
- InvoiceStatus.
- PaymentProviderRef.

Eventos de domínio:

- PlanoCriado.
- AssinaturaCriada.
- CicloDeCobrancaAberto.
- InvoiceEmitida.
- PagamentoConfirmado.
- AssinaturaSuspensa.
- FeatureLimitadaPorPlano.

Dependências:

- Futuramente depende de Tenant como unidade de cobrança.
- Futuramente depende de Feature Governance para PlanFeature.
- Depende de Analytics/limites para consumo.
- Depende de Contratos e Governança quando ativado.

Bounded contexts:

- Billing Context.
- Subscription Context.
- Commercial Packaging Context.

APIs expostas:

- Nenhuma no MVP.
- Futuramente API administrativa/comercial de planos, assinaturas e invoices.

APIs consumidas:

- Futuramente payment provider.
- API de tenants.
- API de features/limits.
- API de analytics/uso.
- API de auditoria.

## Mapa de Dependências Completo

### Relação de Base

- Identity Provider Integration autentica usuários.
- Tenancy, Memberships e Autorização Contextual usa Identity para saber quem é o usuário.
- Tenancy define o espaço onde o usuário pode atuar.
- Gestão de Produtos Digitais depende de Tenancy porque todo produto pertence a exatamente um tenant.
- Feature Governance depende de Produto porque features são habilitadas por produto.
- Todos os domínios tenant-owned dependem de Tenancy, Produto e Autorização.
- Contratos JSON dependem dos domínios de negócio para montar projeções públicas e administrativas.
- API REST expõe os contratos e comandos de cada domínio.

### Dependências do Core

- Produto depende de Tenant, Membership, Feature Governance, Contratos, Auditoria e Revisões.
- Tenant depende de Identity, Memberships, Auditoria, Segurança, LGPD e Notificações.
- Membership depende de Tenant, Produto, Identity, Convites, Auditoria e Notificações.
- Feature Governance depende de Produto, Tenant, Membership, Contratos, Navegação, Auditoria e futuro Billing.
- Content Platform depende de Produto, Tenant, Feature Governance, Revisões, Assets, SEO, i18n, Navegação, Busca, Analytics, Contratos e Auditoria.
- Contratos JSON dependem de Produto, Conteúdo, Assets, Forms, SEO, Navegação, Busca, Analytics, Revisões, Feature Governance e API REST.
- Workflow Editorial depende de Conteúdo, SEO, Navegação, Forms, Assets, i18n, Produto, Tenant, Membership, Contratos, Cache, Busca e Auditoria.

### Dependências de Conteúdo e Entrega Pública

- Assets depende de Produto, Tenant, Features, Storage, Jobs, LGPD, Contratos e Auditoria.
- Forms depende de Produto, Tenant, Features, Assets, LGPD, Notificações, Analytics, Contratos e Auditoria.
- SEO depende de Conteúdo, Produto, i18n, Navegação, Revisões, Contratos, Cache e Auditoria.
- Navegação depende de Conteúdo publicado, Produto, Tenant, Features, Membership, i18n, Revisões e Contratos.
- i18n depende de Produto, Conteúdo, SEO, Navegação, Forms, Assets, Revisões, Feature Limits, Busca e Contratos.
- Busca depende de Conteúdo publicado, Produto, Tenant, locale, Jobs, Membership, Analytics e PostgreSQL FTS.
- Analytics depende de Produto, Tenant, Conteúdo, Forms, Busca, Assets, Jobs, Cache e LGPD.

### Dependências Operacionais

- Notificações depende de eventos de Forms, Memberships, Conteúdo, Comentários, Sugestões e Operação.
- Telegram é provider de Notificações, não domínio próprio.
- Jobs depende dos domínios que enfileiram trabalho: Assets, Notificações, Busca, Analytics, LGPD e Cache.
- Cache depende de Contratos, Conteúdo, Features, SEO, Navegação e Analytics.
- Observabilidade depende de todos os domínios para coleta de sinais.
- Auditoria depende de todos os domínios que executam ações relevantes.
- Segurança depende de Identity, Tenant, Membership, Produto, Features, Auditoria e Observabilidade.
- Banco/Migrations suporta todos os domínios persistentes.
- Deploy depende de Banco/Migrations, Observabilidade, Segurança e ADR-0009.
- Backup/DR depende de PostgreSQL, Keycloak, Storage de Assets, Secrets, Observabilidade, Segurança e LGPD.
- Governança depende da documentação fonte de verdade e governa mudanças estruturais em todos os domínios.

### Dependências dos Domínios Verticais

- Institucional/CMSS depende de Produto, Content Platform, Pages, Navigation, SEO, Assets, Events, Forms, Analytics e Contratos.
- Música/Maestro Beton depende de Produto, Content Types, Assets, Forms, Notificações, Telegram, SEO, Analytics e Contratos.
- RH/Conecta Talentos depende de Produto, Content Types, Forms, Assets sensíveis, LGPD, Notificações, Analytics, Busca e Contratos.
- Portfólio/Alexandre Dev depende de Produto, Content Types, Assets, Downloads, SEO, Analytics, Busca e Contratos.
- Biblioteca/Loki depende de Produto, Content Types, i18n, Assets, Busca, Analytics, Colaboração e Contratos.
- Wiki/WikiDev depende de Produto, Content Types, Navegação, Busca, Colaboração, Sugestões, Bug Reports, Notificações, Analytics e Contratos.

### Dependências Futuras

- GraphQL futuro depende de REST/Contratos estabilizados, dos mesmos serviços de domínio, Permission Evaluator, tenant isolation, persisted queries e limites de complexidade.
- Redis futuro depende de crescimento horizontal e substitui/complementa Caffeine sem mudar domínio.
- Motor de busca dedicado futuro depende de sinais de volume, latência ou necessidade semântica; substitui PostgreSQL FTS como infraestrutura, não como domínio.
- CDN futura depende de escala de entrega pública e usa surrogate keys por tenant/produto.
- Schema por tenant ou banco por tenant depende de demanda enterprise e novo ADR.
- Billing futuro depende de Tenant, Feature Governance, Limits, Analytics de consumo, Contratos e Payment Provider.

### Cadeia Principal de Negócio

Administrador autentica no Identity Provider.

O Aegis resolve memberships e tenants acessíveis.

Usuário seleciona tenant ativo.

Administrador cria ou seleciona produto.

Produto recebe categoria e features.

Features habilitadas montam menus, permissões, limites e contratos disponíveis.

Editor cria conteúdo, assets, forms, SEO e navegação dentro do produto.

Workflow cria revisões, aprova e publica.

Publicação move o ponteiro público para a revisão aprovada.

Contratos JSON são montados e validados.

Cache público é invalidado ou aquecido.

Busca é reindexada.

Frontend externo consome API pública e renderiza o produto.

Analytics registra uso.

Auditoria registra ações relevantes.

Observabilidade permite rastrear request, job, notificação e incidente.

Governança preserva decisões e impede evolução desordenada.
