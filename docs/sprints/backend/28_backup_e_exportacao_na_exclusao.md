# Etapa 28 — Backup e exportação de dados na exclusão de produto ou tenant

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisitos: etapa 12 (domínio assets + `StorageProvider`), etapa 06 (SMTP + `KeycloakAdminClient`), etapa 10 (`DELETE /tenants/{tenantId}`), etapa 11 (content), etapa 22 (pages), etapa 13 (forms), etapa 18 (knowledge graph).

## Contexto

Qualquer ação irreversível de exclusão de produto ou tenant deve garantir que o usuário proprietário **receba uma cópia completa de todos os seus dados antes que qualquer dado seja removido do banco ou do storage**. Isso cobre dois cenários:

1. **`DELETE /api/v1/products/{productId}`** *(endpoint novo — não existe ainda)*: o criador/owner do produto recebe o backup do produto excluído.
2. **`DELETE /api/v1/tenants/{tenantId}`** *(endpoint existente, etapa 10)*: antes de cascatear a exclusão para os produtos, gera um backup por produto e envia para o owner de cada um.

O backup inclui todo o conteúdo do banco de dados associado ao produto (content, pages, forms + submissions, assets, knowledge graph, módulos, assignments, auditoria) mais os arquivos físicos de assets (independente do `storageProvider` ser `local` ou `s3`). O resultado é um arquivo ZIP estruturado enviado via link de download por e-mail (não como anexo — o tamanho pode ser proibitivo).

## A. Endpoints novos

```txt
DELETE /api/v1/products/{productId}
GET    /api/v1/exports/{tokenId}/download   ← download público autenticado por token temporário
```

**`DELETE /api/v1/products/{productId}`** — exclusão de produto com backup prévio obrigatório:

```ts
type DeleteProductRequest = {
  confirmationText: string; // nome exato do produto (product.name)
};
// Response: 202 Accepted
// Body: { "message": "Exportação iniciada. Um link de download será enviado para {email} em instantes." }
```

Sequência obrigatória:
1. Validar `confirmationText === product.name` (senão 400)
2. Verificar que o caller tem `ProductAssignment` com `role: "product_manager"` ou é `TENANT_ADMIN`/`SUPER_ADMIN` do tenant (senão 403)
3. Verificar que o produto não tem exclusão em andamento (idempotência — senão 409 `EXPORT_ALREADY_IN_PROGRESS`)
4. Marcar produto com `status = "deleting"` (novo status — impede uso durante o backup)
5. Retornar **202 Accepted** imediatamente (não bloquear o request)
6. Disparar job assíncrono (`@Async`) → `ExportAndDeleteService.exportAndDelete(productId, callerSubject)`

**`GET /api/v1/exports/{tokenId}/download`** — download do ZIP via token temporário:
- Não requer autenticação JWT (o token é a autorização)
- Valida que `ExportToken.expiresAt > now` (senão 410 Gone — "Este link expirou")
- Valida que `ExportToken.status = "available"` (senão 404)
- Faz streaming do arquivo ZIP diretamente: `Content-Disposition: attachment; filename="..."`
- Registra `ExportToken.downloadedAt` (idempotente — pode baixar mais de uma vez enquanto válido)

## B. Retrofit em `DELETE /tenants/{tenantId}` (etapa 10)

Adicionar à sequência de exclusão de tenant, **antes** de qualquer cascade:

1. Para cada `Product` ativo do tenant (em paralelo, com `CompletableFuture`):
   - Marcar `Product.status = "deleting"`
   - Enfileirar job de export: `ExportAndDeleteService.exportAndDelete(productId, callerSubject)`
2. Os jobs rodam de forma assíncrona. O `DELETE /tenants/{tenantId}` ainda retorna **202 Accepted** (não 204, para sinalizar que a deleção está em andamento)
3. Cada job exporta, envia o e-mail, e só então marca o produto como excluído e limpa os dados
4. Quando todos os produtos estiverem com `status = "deleted"`, o job final exclui o tenant

> **Nota de implementação:** a exclusão em cascata existente na etapa 10 (produtos, módulos, memberships, ProductAssignments) passa a ser controlada pelo `ExportAndDeleteService` — o `TenantService.delete()` não mais deleta em cascata diretamente; apenas triggera os jobs e aguarda completude via status.

## C. Formato do arquivo de exportação

### C.1 Estrutura do ZIP

Padrão de mercado: **JSON por domínio + arquivos físicos** (mesma abordagem do Contentful Export, Webflow CMS Export e Ghost JSON). Cada domínio serializa seus dados em um JSON separado. Assets físicos ficam numa subárvore `assets/files/`, espelhando a estrutura de categorias do storage original.

```
aegis-export-{productKey}-{yyyyMMdd-HHmmss}.zip
│
├── manifest.json               ← metadados do export (primeira coisa a ler)
├── product.json                ← configuração do produto
├── modules.json                ← módulos habilitados + settingsJson de cada um
│
├── content/
│   └── entries.json            ← todas as entradas de conteúdo (com versionamento embutido)
│
├── pages/
│   └── pages.json              ← páginas com seções e blocos inline
│
├── forms/
│   ├── forms.json              ← definições dos formulários
│   └── submissions.json        ← todas as submissões
│
├── assets/
│   ├── metadata.json           ← metadados de cada asset (id, name, type, storageKey, etc.)
│   └── files/
│       ├── image/              ← arquivos físicos por categoria (espelho do storage original)
│       ├── pdf/
│       ├── audio/
│       ├── video/
│       └── document/
│
├── knowledge-graph/
│   ├── nodes.json
│   └── edges.json
│
├── users/
│   └── assignments.json        ← ProductAssignment records (email dos usuários, papéis)
│
└── audit/
    └── events.json             ← trilha de auditoria do produto
```

### C.2 `manifest.json` — metadados do export

```json
{
  "aegisExportVersion": "1.0",
  "exportedAt": "2026-06-26T21:00:00.000Z",
  "trigger": "PRODUCT_DELETED",
  "product": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "key": "maestro-beton",
    "name": "Maestro Beton",
    "type": "Site Institucional",
    "defaultLocale": "pt-BR",
    "assetStorageStrategy": "local",
    "tenantId": "7cf39a8c-...",
    "tenantName": "BYOP"
  },
  "requestedBy": {
    "subject": "keycloak-uuid",
    "email": "alexandre@byop.dev"
  },
  "entityCounts": {
    "contentEntries": 42,
    "pages": 8,
    "forms": 3,
    "formSubmissions": 127,
    "assets": 56,
    "assetFilesBytes": 184320000,
    "knowledgeGraphNodes": 89,
    "knowledgeGraphEdges": 134,
    "auditEvents": 312
  },
  "downloadExpiresAt": "2026-07-03T21:00:00.000Z"
}
```

### C.3 Regras de serialização dos JSONs de domínio

- Todos os JSONs usam **array de objetos** na raiz (nunca objeto com chave `"items"`): `[{...}, {...}]`
- Campos `null` são **omitidos** (não serializados) — reduz tamanho e facilita leitura
- Datas em **ISO 8601 UTC** (`"2026-06-26T21:00:00.000Z"`)
- UUIDs como **string** (nunca número)
- `storageKey` de assets: preservado exatamente como está no banco — permite reimportar no mesmo storage sem remapeamento
- `settingsJson` de módulos: serializado como objeto (não como string escaped)
- Blocos de página: inline no `pages.json` (não como referência separada)
- Versões de conteúdo: inline em `entries.json` como array `"versions": [...]` em cada entry

Exemplo de entrada em `content/entries.json`:
```json
[
  {
    "id": "uuid",
    "contentType": "artigo",
    "slug": "meu-artigo",
    "locale": "pt-BR",
    "status": "publicado",
    "fields": {
      "titulo": "Meu artigo",
      "corpo": "...",
      "imagem": { "assetId": "uuid-do-asset" }
    },
    "versions": [
      { "versionNumber": 1, "publishedAt": "2026-01-10T10:00:00.000Z", "authorSubject": "keycloak-uuid" }
    ],
    "createdAt": "2026-01-10T09:00:00.000Z",
    "updatedAt": "2026-06-20T14:30:00.000Z"
  }
]
```

### C.4 Assets físicos — estratégia de cópia

O `ExportAndDeleteService` usa o `StorageProvider` existente (etapa 12) para **ler** os bytes de cada asset e gravá-los no ZIP via streaming, sem carregar tudo em memória:

- Para `local`: `Files.newInputStream(Path.of(storagePath, asset.storageKey))`
- Para `s3`: `S3Client.getObject(...)` retorna `ResponseInputStream` — streamed direto no `ZipOutputStream`

Caminho dentro do ZIP: `assets/files/{asset.category}/{asset.name}` (nome legível — mesmo padrão do storage local). Se houver colisão de nome (dois assets com mesmo nome e categoria), adicionar o UUID como sufixo: `{name}-{assetId[0..7]}.{ext}`.

## D. Fluxo completo do job assíncrono

`ExportAndDeleteService.exportAndDelete(productId, callerSubject)`:

```
1. [SERIALIZE] Consultar e serializar cada domínio para JSON em memória (ou temp file para grandes volumes):
   → ContentRepository, PageRepository, FormRepository, SubmissionRepository,
     AssetRepository, KnowledgeGraphNodeRepository, KnowledgeGraphEdgeRepository,
     ProductModuleRepository, ProductAssignmentRepository, AuditEventRepository

2. [ZIP] Criar arquivo ZIP em diretório temporário do sistema (não na pasta de produto):
   → Path zipPath = Path.of(exportTempDir, "aegis-export-{productKey}-{timestamp}.zip")
   → Gravar manifest.json, product.json, modules.json e todos os JSONs de domínio
   → Para cada Asset: stream bytes do StorageProvider → ZipOutputStream (sem carregar tudo em RAM)

3. [STORE] Mover ZIP para área de exports permanente:
   → LocalStorageProvider: pasta `exports/{tokenId}/` fora da árvore de produtos
   → S3StorageProvider: chave `aegis/pms/exports/{tokenId}/{filename}.zip`
   → Criar entidade ExportToken no banco

4. [EMAIL] Buscar e-mail do owner (callerSubject) via KeycloakAdminClient
   → Renderizar template `productExport.ftl` com link de download
   → Enviar via JavaMailSender (SMTP configurado na etapa 06)

5. [DELETE] Só após e-mail enviado com sucesso:
   → Revogar ProductAssignments do produto
   → Deletar assets físicos via StorageProvider.delete() para cada asset
   → Deletar estrutura de pastas do produto (local) ou prefixo S3
   → Deletar registros do banco em ordem segura:
      AuditEvent → Submission → Form → Block → Section → Page →
      ContentVersion → ContentEntry → KnowledgeGraphEdge → KnowledgeGraphNode →
      Asset → ProductModule → ProductAssignment → Product

6. [AUDIT] Registrar evento: USER_DATA_EXPORTED_AND_PRODUCT_DELETED
```

**Tratamento de falha no job:**
- Se qualquer passo 1-4 falhar: marcar produto com `status = "export_failed"` (não deletar nada) + logar stacktrace + notificar via e-mail simples ao owner: "Seu backup falhou — entre em contato com o suporte"
- Passo 5 (delete) falha parcialmente: logar, marcar `status = "delete_failed"`, não retentar automaticamente (evitar exclusão parcial silenciosa)
- Retries ficam para operações de infraestrutura (SMTP, S3 upload) com backoff exponencial — máximo 3 tentativas cada

## E. Entidade `ExportToken`

```java
@Entity @Table(name = "export_tokens")
public class ExportToken {
  UUID id;            // PK — é o tokenId na URL de download
  UUID productId;     // referência fraca (produto pode ter sido deletado)
  String productKey;  // para montar o nome do arquivo na resposta
  String zipPath;     // caminho relativo no storage de exports (local ou chave S3)
  String storageProvider; // "local" | "s3"
  String recipientEmail;
  String status;      // "pending" | "available" | "expired"
  Instant createdAt;
  Instant expiresAt;  // createdAt + 7 dias
  Instant downloadedAt; // nullable — primeira vez que foi baixado
}
```

**Limpeza de exports expirados:** job `@Scheduled(cron = "0 0 3 * * *")` rodando à meia-noite + 3h:
1. Buscar todos `ExportToken` com `expiresAt < now` e `status != "expired"`
2. Para cada um: deletar o arquivo ZIP do storage de exports, marcar `status = "expired"`

## F. Template de e-mail — `productExport.ftl`

> Arquivo: `infra/keycloak/themes/aegis/email/html/productExport.ftl`
> (ou `backend/src/main/resources/templates/email/productExport.ftl` se o GPT da etapa escolher renderização direta — registrar a decisão no SPRINT-RESULTADO.md)

Variáveis disponíveis: `${userName}`, `${productName}`, `${tenantName}`, `${downloadUrl}`, `${expiresAt}` (data formatada em pt-BR), `${entityCounts}` (mapa com as contagens), `${fileSizeMb}` (tamanho total do ZIP em MB).

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Exportação de dados — ${productName}</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
    .container { max-width: 580px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .header { background: #1a1a2e; padding: 32px 40px; text-align: center; }
    .header h1 { color: #ffffff; font-size: 20px; margin: 0; font-weight: 600; letter-spacing: -0.3px; }
    .body { padding: 36px 40px; color: #374151; }
    .body p { font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
    .product-card {
      background: #f0fdf4; border: 1px solid #bbf7d0;
      border-radius: 8px; padding: 18px 22px; margin: 24px 0;
    }
    .product-card .label { font-size: 11px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
    .product-card .value { font-size: 17px; font-weight: 700; color: #111827; }
    .product-card .tenant { font-size: 13px; color: #6b7280; margin-top: 2px; }
    .stats { display: table; width: 100%; border-collapse: collapse; margin: 20px 0; }
    .stat-row { display: table-row; }
    .stat-label { display: table-cell; font-size: 13px; color: #6b7280; padding: 5px 0; width: 60%; }
    .stat-value { display: table-cell; font-size: 13px; font-weight: 600; color: #111827; text-align: right; }
    .divider { border: none; border-top: 1px solid #f3f4f6; margin: 24px 0; }
    .cta-block { text-align: center; margin: 28px 0 8px; }
    .btn-download {
      display: inline-block; background: #4f46e5; color: #ffffff;
      text-decoration: none; padding: 14px 36px;
      border-radius: 7px; font-size: 15px; font-weight: 600;
      letter-spacing: -0.2px;
    }
    .btn-sub { font-size: 12px; color: #9ca3af; margin-top: 12px; text-align: center; }
    .warning-box {
      background: #fffbeb; border: 1px solid #fcd34d;
      border-radius: 6px; padding: 14px 18px; margin: 24px 0;
      font-size: 13px; color: #92400e; line-height: 1.5;
    }
    .footer { padding: 22px 40px; border-top: 1px solid #f3f4f6; color: #9ca3af; font-size: 12px; text-align: center; line-height: 1.6; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Aegis PMS</h1>
    </div>

    <div class="body">
      <p>Olá, <strong>${userName}</strong>.</p>
      <p>
        Como solicitado, seu produto foi excluído e <strong>uma cópia completa dos seus dados está pronta para download</strong>.
        O arquivo inclui todo o conteúdo, páginas, formulários, submissões, assets e configurações do produto.
      </p>

      <div class="product-card">
        <div class="label">Produto exportado</div>
        <div class="value">${productName}</div>
        <div class="tenant">${tenantName}</div>
      </div>

      <p style="font-size:13px; color:#374151; font-weight:600; margin-bottom:8px;">O que está incluído:</p>
      <div class="stats">
        <#if (entityCounts.contentEntries > 0)>
        <div class="stat-row">
          <div class="stat-label">Entradas de conteúdo</div>
          <div class="stat-value">${entityCounts.contentEntries}</div>
        </div>
        </#if>
        <#if (entityCounts.pages > 0)>
        <div class="stat-row">
          <div class="stat-label">Páginas</div>
          <div class="stat-value">${entityCounts.pages}</div>
        </div>
        </#if>
        <#if (entityCounts.forms > 0)>
        <div class="stat-row">
          <div class="stat-label">Formulários</div>
          <div class="stat-value">${entityCounts.forms}</div>
        </div>
        </#if>
        <#if (entityCounts.formSubmissions > 0)>
        <div class="stat-row">
          <div class="stat-label">Submissões de formulários</div>
          <div class="stat-value">${entityCounts.formSubmissions}</div>
        </div>
        </#if>
        <#if (entityCounts.assets > 0)>
        <div class="stat-row">
          <div class="stat-label">Assets (imagens, vídeos, docs)</div>
          <div class="stat-value">${entityCounts.assets} · ${fileSizeMb} MB</div>
        </div>
        </#if>
        <#if (entityCounts.knowledgeGraphNodes > 0)>
        <div class="stat-row">
          <div class="stat-label">Nós do Knowledge Graph</div>
          <div class="stat-value">${entityCounts.knowledgeGraphNodes}</div>
        </div>
        </#if>
      </div>

      <hr class="divider">

      <div class="cta-block">
        <a href="${downloadUrl}" class="btn-download">⬇ Baixar meus dados</a>
        <div class="btn-sub">Arquivo ZIP · ${fileSizeMb} MB</div>
      </div>

      <div class="warning-box">
        ⚠️ <strong>Este link expira em ${expiresAt}.</strong>
        Após essa data, o arquivo será removido permanentemente de nossos servidores e não será possível recuperá-lo.
        Faça o download o quanto antes e guarde o arquivo num local seguro.
      </div>

      <p style="font-size:13px; color:#6b7280;">
        Se você não solicitou a exclusão deste produto, entre em contato com o suporte imediatamente —
        seus dados estão seguros e disponíveis pelo link acima até <strong>${expiresAt}</strong>.
      </p>
    </div>

    <div class="footer">
      Aegis PMS · <a href="https://aegis.app" style="color:#9ca3af;">aegis.app</a><br>
      Você recebeu este e-mail porque o produto <em>${productName}</em> foi excluído da sua conta.<br>
      Este link de download é pessoal e intransferível.
    </div>
  </div>
</body>
</html>
```

## G. Classes Java a implementar

| Classe | Responsabilidade |
|---|---|
| `ExportToken` (entity) | Registro de download temporário (Seção E) |
| `ExportTokenRepository` | `findByIdAndStatusAndExpiresAtAfter(...)`, `findExpired()` |
| `ProductExportSerializer` | Serializa cada domínio para `byte[]` JSON via Jackson `ObjectMapper` |
| `ExportZipBuilder` | Monta o ZIP em arquivo temporário usando `ZipOutputStream` com streaming de assets |
| `ExportStorageService` | Grava/lê/deleta ZIPs na área de exports (abstrai local vs S3, reutiliza `StorageProvider`) |
| `ExportAndDeleteService` | Orquestra o fluxo completo (Seção D); `@Async` |
| `ExportCleanupJob` | `@Scheduled` — expira e deleta ZIPs vencidos |
| `ExportController` | `GET /api/v1/exports/{tokenId}/download` |
| `ProductDeleteController` | `DELETE /api/v1/products/{productId}` |

`ExportAndDeleteService` **não** conhece detalhes de serialização nem de storage — delega para `ProductExportSerializer` e `ExportZipBuilder`. Cada service do domínio (ContentService, PageService, etc.) expõe um método `findAllByProductId(productId)` que o `ProductExportSerializer` usa — sem acoplamento adicional.

## H. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. 100% de cobertura nas classes funcionais desta etapa.
- `ExportAndDeleteService` usa `@Async` com um `ThreadPoolTaskExecutor` configurado explicitamente (não o default do Spring) — documentar o tamanho do pool e o nome no SPRINT-RESULTADO.md.
- `ExportZipBuilder` usa streaming em todo momento: nunca carrega mais de um asset inteiro em RAM; usa `ZipOutputStream` com buffer de 8KB.
- O `GET /api/v1/exports/{tokenId}/download` não lê o ZIP inteiro para memória: usa `StreamingResponseBody` + `HttpHeaders.CONTENT_DISPOSITION` para stream do arquivo diretamente.
- Entregar em rodadas:
  1. `ExportToken` (entity) + `ExportTokenRepository` + `ExportCleanupJob` + testes `@DataJpaTest` e testes do job com mock de repositório.
  2. `ProductExportSerializer` + `ExportZipBuilder` — testes unitários com produto fictício (sem banco real): serializar para bytes, montar ZIP em memória (`ByteArrayOutputStream`), verificar presença de cada entrada esperada.
  3. `ExportStorageService` — testes com provider mockado (não escrever em disco real nos testes); testar path de local e de S3 separadamente.
  4. `ExportAndDeleteService` (`@Async`) — testes com todos os colaboradores mockados; cobrir: fluxo feliz completo, falha no SMTP (produto fica `export_failed`), falha no delete (produto fica `delete_failed`).
  5. `ExportController` + `ProductDeleteController` + testes `@WebMvcTest`; curl de validação.

## I. Critérios de aceite

- [ ] `DELETE /products/{productId}` retorna **202** imediatamente, não bloqueia.
- [ ] ZIP gerado tem a estrutura exata da Seção C.1 (verificar com `unzip -l` no teste de integração).
- [ ] `manifest.json` dentro do ZIP contém `entityCounts` com contagens corretas.
- [ ] Assets físicos estão em `assets/files/{category}/` dentro do ZIP, independente do `storageProvider` ser `local` ou `s3`.
- [ ] Link de download funciona sem autenticação JWT e retorna o ZIP com header `Content-Disposition: attachment`.
- [ ] Link de download retorna **410 Gone** após `expiresAt`.
- [ ] Job de limpeza (`ExportCleanupJob`) deleta o ZIP do storage e marca token como `"expired"`.
- [ ] `DELETE /products/{productId}` com `confirmationText` errado retorna 400.
- [ ] Produto em status `"deleting"` ou `"export_failed"` rejeita nova tentativa com 409.
- [ ] Falha no envio do e-mail: produto fica `export_failed`, **nenhum dado é deletado**.
- [ ] `DELETE /tenants/{tenantId}` dispara export para cada produto do tenant antes de qualquer cascade.
- [ ] E-mail recebido no MailHog (dev) tem o template correto com link, contagens e data de expiração.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).

## J. Validação

> **Entrega via collection Bruno** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11).

```bash
# Excluir produto (retorna 202, dispara job assíncrono)
curl -X DELETE http://localhost:8080/api/v1/products/<productId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"confirmationText":"Maestro Beton"}'
# esperado: 202 com mensagem de exportação iniciada

# Abrir MailHog: http://localhost:8025
# Copiar tokenId do link de download no e-mail

# Baixar o ZIP (sem Authorization header)
curl -L "http://localhost:8080/api/v1/exports/<tokenId>/download" -o backup.zip

# Verificar estrutura
unzip -l backup.zip

# Verificar manifest
unzip -p backup.zip "*/manifest.json" | python3 -m json.tool

# Tentar baixar depois de expirado (esperado: 410)
curl -v "http://localhost:8080/api/v1/exports/<tokenId-expirado>/download"
```

## L. Cleanup de stubs órfãos no código de produção (TODOs sem sprint)

Uma auditoria do código identificou dois stubs marcados com `// TODO Sprint futura` que **não têm sprint prevista** e estão incorretamente localizados em `src/main/java` sem serventia alguma em produção:

| Classe | Situação atual | Implementação real existente |
|---|---|---|
| `StubProductAssignmentEmailPort` | `src/main/java`, sem `@Component` — inativo em produção, mas polui o classpath | `FreemarkerProductAssignmentEmailPort` (`@Component`) — já ativo |
| `StubProductAssignmentInvitePort` | `src/main/java`, sem `@Component` — inativo em produção | `KeycloakProductAssignmentInvitePort` (`@Component`) — já ativo |

> `StubProductAssignmentNotificationPort` tem `@Component` e o TODO referencia **Sprint 25** explicitamente — esta classe está coberta e não entra neste cleanup.

**Ação obrigatória nesta etapa:**

1. Mover `StubProductAssignmentEmailPort` de `src/main/java/.../service/` para `src/test/java/.../service/`
2. Mover `StubProductAssignmentInvitePort` de `src/main/java/.../service/` para `src/test/java/.../service/`
3. Verificar que `ProductAssignmentPortStubTest` (que os instancia diretamente, sem injeção) continua compilando e passando após a mudança — os stubs em `src/test` continuam acessíveis pelos testes
4. Confirmar que `FreemarkerProductAssignmentEmailPort` e `KeycloakProductAssignmentInvitePort` continuam sendo os únicos beans registrados para suas respectivas interfaces em produção (`mvn spring-boot:run` + `GET /actuator/beans` se disponível)
5. Remover os comentários `// TODO Sprint futura` após a movimentação (o TODO foi resolvido)

Esta tarefa é de limpeza de código — sem Flyway migration, sem endpoint novo, sem teste extra além de confirmar que os testes existentes passam.

## K. Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Registrar obrigatoriamente no SPRINT-RESULTADO.md:
> - Tamanho do pool do `ThreadPoolTaskExecutor` escolhido e justificativa
> - Onde os templates `.ftl` foram colocados (tema Keycloak vs `resources/templates/`)
> - Estratégia de storage dos exports (subdiretório de local vs prefixo S3)
> - Tempo de expiração efetivo adotado (o padrão recomendado é 7 dias)

## Commit sugerido

```bash
git add backend/ infra/keycloak/themes/
git commit -m "feat(backend): backup e exportacao zip na exclusao de produto e tenant"
```
