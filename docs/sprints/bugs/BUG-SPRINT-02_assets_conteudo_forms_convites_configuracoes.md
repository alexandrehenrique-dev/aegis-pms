# Bug Sprint 02 — Assets, Conteúdo, Forms, Convites e Configurações

> **Contexto:** Bateria de testes manuais realizada pelo PO em 05/07/2026 cobrindo os domínios Assets, Conteúdo, Forms, Analytics, Configurações/Segurança e fluxo de convite por produto. Foram encontrados 26 bugs, 4 deles críticos (dados de contexto errados no email de convite). O agente deverá realizar varredura completa de cada tela afetada antes de implementar — a instrução explícita é: **não implementar nada sem antes passar pelo código fonte e confirmar a causa raiz**.
>
> **Branch sugerida:** `bugfix/sprint-02-assets-conteudo-forms-convites`
>
> **Padrão de qualidade obrigatório:**
> - Novo endpoint backend → Bruno collection + JaCoCo 100% (line e branch)
> - Novo componente/page frontend → tsc --noEmit zero erros + smoke test manual documentado
> - Nenhum valor hardcoded que dependa do produto/tenant efetivo da sessão

---

## Diagnóstico de ambiente

Antes de iniciar qualquer tarefa, validar:

```bash
# 1. Backend UP
curl -s http://localhost:8080/actuator/health | jq '.status'
# Esperado: "UP"

# 2. TypeScript sem erros
cd frontend && npm run typecheck
# Esperado: saída vazia (0 erros)

# 3. Maven + JaCoCo
cd backend && mvn verify -q
# Esperado: BUILD SUCCESS + jacoco-check PASSED

# 4. MailHog acessível
curl -s http://localhost:8025/api/v2/messages | jq '.total'
# Esperado: número >= 0 (sem erro de conexão)
```

---

## Seção A — Assets (7 bugs)

### A.1 — AssetDetail completamente mockado (componente `AssetPreviewPanel` estático)

**Sintoma:** Clicar em qualquer asset na biblioteca abre uma tela com preview e metadados hardcoded: `hero-maestro-beton.jpg`, `1920×1080`, `2.4 MB`, `checksum: ags_42f9`, `imagem`, `zoom 100%`. Independente do asset clicado.

**Causa raiz (confirmada em código):**
```tsx
// frontend/src/domains/assets/pages/AssetDetail.tsx
function AssetPreviewPanel() {
  return (
    <Card>
      <div className="aspect-video ...">
        <Badge tone="green">imagem</Badge>   {/* ← hardcoded */}
        <h2>hero-maestro-beton.jpg</h2>       {/* ← hardcoded */}
        <p>Preview grande do arquivo...</p>
      </div>
      <div ...><Badge>1920×1080</Badge><Badge>2.4 MB</Badge><Badge>checksum: ags_42f9</Badge></div>
    </Card>
  );
}

// AssetDetail não recebe o assetId por parâmetro de rota — está hardcoded:
const assetName = "hero-maestro-beton.jpg";  // ← linha ~32, hardcoded
```

**Fix necessário:**
1. Ler `assetId` de `useParams()` (`:assetId` já deve existir na rota).
2. Chamar `assetsService.getAsset(productId, assetId)` — verificar se o endpoint `GET /products/{productId}/assets/{assetId}` existe no backend; se não, criá-lo.
3. Renderizar `AssetPreviewPanel` dinamicamente baseado no `mimeType` retornado:
   - `image/*` → `<img>` com controles de zoom
   - `video/*` → `<video controls>`
   - `audio/*` → `<audio controls>`
   - `application/pdf` → `<iframe>` ou `<embed>`
   - outros → ícone genérico de documento com nome e tamanho
4. Sidebar direita (Metadados): exibir `tipo`, `tamanho`, `dimensões` (se imagem/vídeo), `status`, `uploadedAt`, `altText`, `crédito`, `tags`, `URL futura` — todos vindos da API.

**Critério de aceite:**
- [ ] Clicar em `hero-maestro-beton.jpg` → preview real da imagem com metadados reais.
- [ ] Clicar em `reflexao.pdf` → preview do PDF (iframe ou embed) com tipo "PDF" na sidebar.
- [ ] `4.png` → imagem real; sidebar mostra "image · 574 KB".
- [ ] Asset de vídeo → player real.
- [ ] tsc --noEmit: zero erros.

**Smoke test:**
```
1. Login: super-admin / senha → produto Maestro Beton → Assets
2. Clicar em qualquer asset → verificar que preview corresponde ao tipo real
3. Sidebar direita deve mostrar metadados reais (não "2.4 MB" fixo)
4. Clicar em asset de tipo diferente → preview muda conforme o tipo
```

---

### A.2 — Download de asset baixa arquivo genérico "download" em vez do asset real

**Sintoma:** Clicar em "Baixar" na tela de detalhe do asset baixa um arquivo sem nome e sem extensão chamado literalmente `download`.

**Causa raiz (confirmada em código):**
```tsx
// frontend/src/domains/assets/pages/AssetDetail.tsx — linha ~71
<a href={assetsService.getDownloadUrl(assetName)} target="_blank" rel="noreferrer">
  <Button primary>Baixar</Button>
</a>

// assetsService.ts — linha ~108
getDownloadUrl(assetId: string): string {
  return `${resolveBaseUrl()}/assets/${assetId}/download`;
  // ↑ sem Content-Disposition no backend → browser não infere nome do arquivo
}
```

**Fix necessário:**
1. O endpoint `GET /assets/{assetId}/download` no backend deve retornar o header `Content-Disposition: attachment; filename="<nome-original>"` com o `Content-Type` correto.
2. Verificar se o endpoint existe e retorna esses headers. Se não, adicionar `ResponseEntity` com headers corretos.
3. No frontend, o `href` + `download` attribute do `<a>` pode complementar: `<a href={url} download={asset.name}>`.
4. Após A.1 ser corrigido, `assetName` passa a ser o nome real do asset (não hardcoded).

**Critério de aceite:**
- [ ] Clicar "Baixar" em `hero-maestro-beton.jpg` → download de arquivo com nome `hero-maestro-beton.jpg`.
- [ ] PDF → download com extensão `.pdf`.
- [ ] Arquivo `.mov` → download com extensão `.mov`.

**Backend — se endpoint não existir:**
```java
@GetMapping("/{assetId}/download")
public ResponseEntity<Resource> downloadAsset(@PathVariable UUID assetId) {
    AssetFile file = assetService.getFile(assetId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.originalName() + "\"")
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .body(file.resource());
}
```
→ Criar Bruno request `GET /products/{productId}/assets/{assetId}/download` + teste JaCoCo.

---

### A.3 — AssetLibrary não abre detalhe ao clicar no asset

**Sintoma:** Clicar no card de um asset na biblioteca ou não abre nada, ou abre a tela de detalhe com o mock fixo (A.1). A navegação não passa o `assetId` real pela rota.

**Causa raiz:** Verificar em `AssetLibrary.tsx` e `AssetUploadScreen.tsx` se `navigate(`/assets/${asset.id}`)` está sendo chamado com o ID real ou com o slug hardcoded. O componente `AssetDetail` usa `const assetName = "hero-maestro-beton.jpg"` — a rota `:assetId` nunca é lida.

**Fix necessário:**
1. Garantir que `AssetLibrary` navega para `/assets/${asset.id}` onde `asset.id` vem da API.
2. `AssetDetail` lê `const { assetId } = useParams()` e usa para buscar o asset real.
3. Verificar se a rota `/assets/:assetId` está configurada no router.

**Critério de aceite:**
- [ ] Clicar em `reflexao.pdf` → URL muda para `/assets/<uuid-do-pdf>`.
- [ ] Clicar em `4.png` → URL muda para `/assets/<uuid-do-png>`.
- [ ] Cada asset abre seus próprios dados (não o mesmo mock).

---

### A.4 — AssetUploadScreen cheia de mocks na sidebar direita

**Sintoma:** A tela de upload (Assets → Upload de Asset) exibe metadados de demonstração na sidebar direita: "Hero Maestro Beton", "Maestro Beton em apresentação ao vivo", "BYOP Studio", "hero, seo, institucional", "Campanha institucional" — todos valores fixos no código, não relacionados ao arquivo sendo enviado.

**Causa raiz:** `AssetUploadScreen.tsx` usa campos pré-preenchidos com dados de exemplo que nunca são limpos após o upload de um arquivo novo. Verificar se os campos de `Nome amigável`, `Alt text`, `Legenda`, `Crédito`, `Tags`, `Pasta lógica / grupo`, `Visibilidade` e `SEO usage` têm valores default hardcoded.

**Fix necessário:**
1. Todos os campos de metadados devem iniciar vazios (string `""`) ou com placeholder descritivo, nunca com dados de exemplo.
2. `Nome amigável` pode ser pré-preenchido com o nome do arquivo sem extensão (bom UX), mas editável.
3. `Alt text` deve iniciar vazio.
4. Após upload concluído, re-inicializar o form.

**Critério de aceite:**
- [ ] Abrir tela de upload → todos os campos de metadados em branco (exceto Nome amigável que pode sugerir o nome do arquivo).
- [ ] Fazer upload de `foto.jpg` → "Nome amigável" sugere "foto", demais campos vazios.
- [ ] tsc --noEmit: zero erros.

---

### A.5 — AssetPicker (upload rápido) — asset enviado não aparece para seleção imediata

**Sintoma:** Na modal "Selecionar asset" (usada ao criar bloco no editor de páginas), o botão "Upload rápido" envia o arquivo mas o asset não aparece na lista da modal para ser selecionado — e o botão "Usar asset" permanece desabilitado.

**Causa raiz:** `AssetPicker` faz upload via `assetsService.upload()` mas não re-executa `assetsService.listAssets(productId)` após o upload concluir. A lista fica com o snapshot anterior. Adicionalmente, em modo API, o backend retorna o asset criado mas o frontend não o adiciona ao estado local.

**Fix necessário:**
1. Após `assetsService.upload()` resolver com sucesso, chamar `assetsService.listAssets(productId)` e atualizar o estado da lista na modal.
2. OU: adicionar o asset recém-criado diretamente ao estado local (otimistic update) se a resposta do upload retornar o objeto completo.
3. Habilitar botão "Usar asset" assim que pelo menos um asset estiver selecionado.

**Critério de aceite:**
- [ ] Upload rápido de `foto.jpg` na modal → `foto.jpg` aparece imediatamente na lista.
- [ ] Clicar em `foto.jpg` → botão "Usar asset" habilitado.
- [ ] Clicar "Usar asset" → bloco recebe o asset corretamente.

---

### A.6 — AssetLibrary: ausência de preview inline e mocks remanescentes

**Sintoma:** A tela de biblioteca de assets (`/assets`) não exibe preview inline dos assets nos cards. Cards mostram ícone genérico de documento independente do tipo real do arquivo.

**Causa raiz:** Os cards de asset usam ícone fixo em vez de `<img src={asset.thumbnailUrl}>` ou similar. O backend deve retornar `thumbnailUrl` ou o frontend deve construir a URL de preview a partir do `assetId`.

**Fix necessário:**
1. Para assets do tipo `image/*`: exibir miniatura via `GET /products/{productId}/assets/{assetId}/thumbnail` (verificar se existe) ou direto via URL pública do storage.
2. Para `video/*`: exibir frame estático ou ícone de vídeo diferenciado.
3. Para `application/pdf`: ícone PDF com cor diferenciada.
4. Para outros tipos: ícone genérico por categoria.
5. Remover quaisquer dados mock remanescentes nos cards (nomes, tamanhos, tags fixas).

**Critério de aceite:**
- [ ] `hero-maestro-beton.jpg` → card exibe miniatura da imagem.
- [ ] `reflexao.pdf` → card exibe ícone PDF + nome real + tamanho real.
- [ ] Assets carregam da API (não do mock em memória) quando `IS_API_MODE=true`.

---

### A.7 — Erro parcial "dados de conversão indisponíveis" recorrente em múltiplas telas

**Sintoma:** Widget vermelho "Erro parcial: dados de conversão indisponíveis. Os demais widgets continuam operacionais." aparece nas telas de Assets, Conteúdo e Forms.

**Causa raiz:** Algum endpoint de analytics/conversão chamado nessas telas retorna erro (provavelmente 404 ou 500), e o componente de widget não trata o erro graciosamente — exibe banner de erro em vez de `null` ou placeholder.

**Fix necessário:**
1. Identificar qual endpoint gera o erro (provavelmente `GET /products/{id}/analytics/conversion` ou similar).
2. Se o endpoint não existir ainda: o widget deve renderizar `null` (invisível) em vez de banner de erro.
3. Se existir mas retornar 500: corrigir o backend ou suprimir o erro no widget com `catch(() => null)`.
4. Regra geral: widgets parciais de analytics nunca devem quebrar a tela principal com banner vermelho — devem aparecer como `—` ou sumir.

**Critério de aceite:**
- [ ] Nenhum banner vermelho de "dados de conversão" nas telas de Assets, Conteúdo ou Forms.
- [ ] Widget de conversão: exibe `—` ou fica invisível quando dados não disponíveis.

---

## Seção B — Conteúdo (5 bugs)

### B.1 — Dashboard editorial mostra KPIs mock; Lista e Workflow aparecem vazios

**Sintoma:** `EditorialDashboard` exibe "8 Rascunhos", "5 Em revisão", "42 Publicados", "7 Arquivados", "11 Traduções pendentes", "14 Atualizados recentemente", "4 Aprovações pendentes" — dados inventados. Mas ao clicar nas abas "Lista" e "Workflow", as tabelas aparecem vazias ("Busca sem resultado" / "Lane vazia").

**Causa raiz:** `EditorialDashboard` usa dados do `contentService` que em modo API retorna lista real (vazia para produto novo) mas os KPI cards são calculados a partir de constantes mock em vez de derivar da lista retornada. A divergência é: a lista é real (vazia), os KPIs são mock (cheios).

**Fix necessário:**
1. Os KPI cards devem derivar dos dados retornados por `contentService.listContent(productId)`:
   - `rascunhos` = `content.filter(c => c.status === "Draft").length`
   - `emRevisao` = `content.filter(c => c.status === "In Review").length`
   - etc.
2. Se os dados reais forem zero (produto sem conteúdo), exibir zeros — não valores mock.
3. Remover qualquer import de dados mock hardcoded nos KPI cards do `EditorialDashboard`.
4. "Atividade editorial recente" (`editEvents`): verificar se `contentService.listEditEvents()` está chamando a API real (`GET /products/{id}/content/edit-events`).

**Critério de aceite:**
- [ ] Produto sem conteúdo: todos os KPIs exibem `0`.
- [ ] Criar um conteúdo → Dashboard mostra "1 Rascunho".
- [ ] Conteúdo aprovado → aparece em "Publicados" no Dashboard E na aba Lista.
- [ ] Dashboard, Lista e Workflow mostram os mesmos dados (mesma fonte).

**Smoke test:**
```
1. Login → Maestro Beton → Conteúdo → Dashboard
2. Verificar KPIs: se produto novo, todos devem ser 0
3. Criar conteúdo novo → voltar ao Dashboard → "1 Rascunho"
4. Aba Lista → mesmo conteúdo deve aparecer
5. Aba Workflow → card na coluna Draft
```

---

### B.2 — Editor consegue publicar conteúdo no Workflow (verificar permissão)

**Sintoma:** Logado como Editor, foi possível mover conteúdo de "In Review" para "Published" no Workflow. As regras do fluxo na sidebar do Workflow dizem "In Review → Published: PM ou superior".

**Investigação necessária:**
1. Verificar `WorkflowBoard.tsx`: o botão/drag de transição `In Review → Published` está protegido por `PermGate` ou `viewAsRole`?
2. Verificar `contentService.publishContent()`: o backend aplica verificação de role em `POST /products/{id}/content/{contentId}/transition`?
3. Se o frontend permite mas o backend bloqueia: comportamento correto (backend é a fonte de verdade) mas UX ruim — o frontend deve esconder a opção para Editor.
4. Se o backend também permite: bug de permissão em ambas as camadas.

**Fix conforme investigação:**
- Caso frontend permita indevidamente: adicionar `PermGate allowed={['product_manager', 'tenant_admin', 'super_admin']}` na ação de publicação.
- Caso backend permita indevidamente: adicionar verificação de role em `ContentTransitionService`.

**Critério de aceite:**
- [ ] Editor logado → Workflow → tentar mover de "In Review" para "Published" → ação bloqueada com mensagem "Sem permissão".
- [ ] Product Manager → mesmo fluxo → publicação permitida.

---

### B.3 — Preview do Workflow mostra mock em vez do conteúdo real; tela precisa de redesign

**Sintoma:** Clicar em "Preview" no card do Workflow abre `ResponsivePreviewFrame` com conteúdo genérico placeholder ("Nenhuma página com slug X encontrada") em vez do conteúdo real.

**Causa raiz:** `ResponsivePreviewFrame` busca por `slug` de página relacionada ao conteúdo, mas o conteúdo de teste não tem página associada OU o `contentId` não está sendo passado corretamente para a busca.

**Fix necessário:**
1. Identificar como `ResponsivePreviewFrame` recebe o conteúdo — é via `contentId`? `slug`? `body`?
2. Renderizar o `body` do conteúdo diretamente (markdown renderizado) se não houver página associada.
3. **Redesign visual obrigatório:** a tela de preview deve ter identidade visual do Aegis:
   - Header com logo Aegis + nome do conteúdo + status badge.
   - Controles de viewport (mobile/tablet/desktop) já existem — mantê-los.
   - Seletor de idioma — manter.
   - Conteúdo renderizado em área central com max-width adequado.
   - Fundo branco/neutro, tipografia limpa.
   - Botões "Voltar ao editor" e "Enviar para revisão" no header.
4. Tela deve ser bonita, alinhada, e refletir o conteúdo real — **não um protótipo**.

**Critério de aceite:**
- [ ] Preview de conteúdo "Teste" (status Published) → exibe o body real do conteúdo renderizado.
- [ ] Viewport mobile → layout compacto; desktop → layout completo.
- [ ] Header com logo Aegis, nome do produto, nome do conteúdo e status.
- [ ] Nenhum texto "Preview indisponível" para conteúdo que existe.
- [ ] tsc --noEmit: zero erros.

---

### B.4 — Toast de "conflito de edição" ao criar nova página é mock

**Sintoma:** Ao criar uma nova página, aparece toast/banner: "Conflito de edição: outro editor alterou este conteúdo há 2 min. Revise antes de publicar." — para um conteúdo recém-criado que nunca foi editado por ninguém.

**Causa raiz:** `ContentEditor.tsx` ou `PageEditor.tsx` exibe o aviso de conflito a partir de dado mock/hardcoded, não de um evento real de conflito (WebSocket ou polling). Para conteúdo recém-criado, nunca deve haver conflito.

**Fix necessário:**
1. Localizar o componente que exibe o banner de conflito.
2. O banner só deve aparecer se `lastModifiedBy !== currentUser` E `lastModifiedAt > sessionStartedAt`.
3. Para conteúdo novo (sem histórico), ocultar o banner.
4. Se não há endpoint real de detecção de conflito, remover o banner até que seja implementado — não exibir dado falso.

**Critério de aceite:**
- [ ] Criar nova página → zero banners de conflito.
- [ ] Abrir página existente que foi editada por outro usuário recentemente → banner aparece com dados reais.

---

### B.5 — Criar página com nome duplicado bloqueia silenciosamente sem feedback

**Sintoma:** Tentar criar uma página com o mesmo nome que outra existente não exibe nenhuma mensagem de erro. O botão "Criar página" simplesmente não executa a ação (ou retorna erro 409 silenciosamente).

**Causa raiz:** O handler de submit do modal "Novo conteúdo" não trata erro de duplicidade (`409 Conflict` ou validação frontend). O catch do `contentService.createContent()` provavelmente ignora o erro ou não exibe toast.

**Fix necessário:**
1. No frontend: tratar `catch` do `contentService.createContent()`:
   - HTTP 409 → `toast.error("Já existe uma página com este título. Escolha outro nome.")`.
   - Outros erros → `toast.error("Erro ao criar página. Tente novamente.")`.
2. No backend: garantir que `POST /products/{id}/content` retorna `409 Conflict` com body `{"error": "DUPLICATE_TITLE"}` quando o título já existe no produto.
3. Validação frontend opcional: verificar duplicidade na lista local antes de submeter.

**Critério de aceite:**
- [ ] Criar "Teste" → criar "Teste" novamente → toast vermelho: "Já existe uma página com este título."
- [ ] Modal permanece aberta para correção (não fecha ao falhar).
- [ ] Backend: `POST /content` com título duplicado → HTTP 409.

---

## Seção C — Forms (2 bugs)

### C.1 — FormsDashboard exibe KPIs falsos (usuário não criou nenhum form)

**Sintoma:** `FormsDashboard` mostra "4 Formulários ativos", "439 Respostas recebidas", "7.4% Conversão média", "16 Respostas pendentes", "74 Leads qualificados", "Timeline Forms" com eventos recentes — tudo falso. O usuário não criou nenhum formulário neste produto.

**Causa raiz:** `FormsDashboard` usa mocks do `forms.mocks.ts` mesmo quando `IS_API_MODE=true`. O serviço `formsService.listForms(productId)` faz a chamada real mas os KPI cards podem estar usando `submissionsData` hardcoded do mock em vez dos dados retornados.

**Fix necessário:**
1. Todos os KPIs devem derivar de `formsService.listForms(productId)`:
   - `formuláriosAtivos` = `forms.filter(f => f.status === 'published').length`
   - `respostasRecebidas` = somatório de `form.submissionsCount` (campo a ser retornado pela API)
2. "Timeline Forms" deve vir de `formsService.listRecentActivity(productId)` ou similar — se não existir, ocultar a seção.
3. Para produto sem forms: todos os KPIs exibem `0`, timeline vazia.
4. Remover import de `submissionsData` do mock nos componentes de dashboard.

**Critério de aceite:**
- [ ] Produto sem formulários → todos os KPIs mostram `0` e "Sem formulários" nas seções vazias.
- [ ] Criar formulário → "1 Formulário ativo" no dashboard.
- [ ] tsc --noEmit: zero erros.

---

### C.2 — Publicar formulário não redireciona conforme jornada

**Sintoma:** Clicar em "Publicar" no Form Builder mantém o usuário na mesma tela. Deveria redirecionar para a lista de formulários ou para a tela de publicação/delivery.

**Causa raiz:** `formsService.publishForm()` em modo API chama `POST /products/{id}/forms/{formId}/publish` mas o handler de sucesso no componente provavelmente não chama `navigate()`.

**Fix necessário:**
1. Localizar o handler de "Publicar" no `FormBuilder.tsx` ou `PublicationPanel.tsx`.
2. Após `formsService.publishForm()` resolver com sucesso:
   ```ts
   toast.success("Formulário publicado!");
   navigate(`/forms`);  // ou navigate(`/forms/${formId}`)
   ```
3. Verificar a jornada definida nos documentos de sprint para decidir para onde navegar.

**Critério de aceite:**
- [ ] Clicar "Publicar" em formulário → toast "Formulário publicado!" → navega para `/forms` ou detalhe do form.
- [ ] Formulário aparece com status "publicado" na lista.

---

## Seção D — Configurações e Segurança (3 bugs)

### D.1 — SecuritySettingsPanel: todas as integrações mostram "Integração mock — Sprint 07"

**Sintoma:** Cada integração em Configurações → Segurança (Webhooks, Analytics Provider, Storage Provider, Alertas Telegram, etc.) exibe modal com: "Integração mock — a configuração real desta integração chega quando o backend (Sprint 07) estiver disponível." A Sprint 07 foi implementada há muito tempo.

**Causa raiz (confirmada em código):**
```tsx
// frontend/src/domains/settings/pages/SecuritySettingsPanel.tsx — linha ~160
<p className="text-sm text-muted-foreground">
  Integração mock — a configuração real desta integração chega
  quando o backend (Sprint 07) estiver disponível.
</p>
```

**Fix necessário:**
1. Mapear cada integração para seu endpoint real (verificar o backend):
   - **Webhooks**: `GET/PUT /tenants/{id}/integrations/webhooks`
   - **Analytics Provider**: `GET/PUT /tenants/{id}/integrations/analytics`
   - **Storage Provider**: já deve existir (assets funcionam) — `GET /tenants/{id}/integrations/storage`
   - **Alertas Telegram**: `GET/PUT /tenants/{id}/integrations/telegram`
2. Substituir o texto mock pelo form real de cada integração.
3. Se algum endpoint realmente não existir ainda, exibir "Em breve" com badge `futuro` — não mencionar "Sprint 07".
4. Remover toda referência a número de sprint no frontend.

**Critério de aceite:**
- [ ] Configurar Webhooks → modal exibe form real de URL + secret.
- [ ] Salvar → `PUT /tenants/{id}/integrations/webhooks` retorna 200.
- [ ] Integração realmente não implementada → badge "Em breve", sem texto de sprint.
- [ ] Zero referências a "Sprint 07" no frontend.

---

### D.2 — Filtros da aba Segurança estão estáticos (devem ser dinâmicos como AuditTimeline)

**Sintoma:** A aba Segurança em Configurações tem sidebar de filtros com chips/botões estáticos que não funcionam. Deve ter a mesma identidade visual e comportamento dos filtros dinâmicos implementados em `AuditTimeline` (ontem).

**Causa raiz:** `SecuritySettingsPanel` tem filtros renderizados como HTML estático sem state/handlers. O padrão correto já existe em `AuditTimeline.tsx` com `FilterPanel`, `FILTER_CONFIG`, `useState<Record<FilterKey, string>>` e chips com `active` state.

**Fix necessário:**
1. Reutilizar ou replicar o padrão `FilterPanel` do `AuditTimeline.tsx` em `SecuritySettingsPanel`.
2. Filtros aplicáveis em Segurança (confirmar pela UI):
   - Por tipo de integração (Webhook, Analytics, Storage, Telegram...)
   - Por status (conectado, requer atenção, futuro, configurado)
3. Ao selecionar filtro, a lista de integrações filtra dinamicamente.
4. Identidade visual: exatamente igual à `AuditTimeline` — chips com cor primária quando ativos, ícone Lucide, "Todos" reseta, "Limpar" remove todos os filtros.

**Critério de aceite:**
- [ ] Chip "conectado" → exibe apenas integrações com status `connected`.
- [ ] Chip "requer atenção" → exibe apenas as com atenção necessária.
- [ ] "Todos" → mostra todas as integrações.
- [ ] Visual idêntico ao `AuditTimeline` (mesmo padding, cores, tamanho de fonte).
- [ ] tsc --noEmit: zero erros.

---

### D.3 — SettingsOverview: "2 usuários com convite pendente" é mock hardcoded

**Sintoma:** Seção "Atenção administrativa" em Configurações → Visão Geral exibe "2 usuários com convite pendente." como texto fixo — não reflete os convites reais do tenant.

**Causa raiz (confirmada em código):**
```tsx
// frontend/src/domains/settings/pages/SettingsOverview.tsx — linha ~14
["2 usuários com convite pendente.", "/users"],
// ↑ hardcoded — nunca muda
```

**Fix necessário:**
1. Buscar via `usersService.listPendingInvites(tenantId)` (verificar se existe; se não, usar `GET /tenants/{id}/users?status=invited`).
2. Se `pendingCount > 0`: exibir "N usuário(s) com convite pendente." como link para `/users`.
3. Se `pendingCount === 0`: remover o item da lista de atenção.
4. Fazer o mesmo para outros itens hardcoded da seção "Atenção administrativa" — verificar se todos são mock.

**Critério de aceite:**
- [ ] Tenant sem convites pendentes → item não aparece na lista de atenção.
- [ ] Enviar convite → item aparece: "1 usuário com convite pendente."
- [ ] Aceitar convite → item desaparece.

---

## Seção E — Analytics (1 bug)

### E.1 — Todas as telas de Analytics usam dados mock

**Sintoma:** `ProductHealth`, `Form Analytics`, `ContentAnalytics`, `Canais`, `Relatórios`, `Tendências` exibem valores que não mudam independente de qual produto está ativo ou do que foi criado. São dados mock de demonstração.

**Causa raiz:** Os serviços de analytics provavelmente caem no branch mock (não `IS_API_MODE`) ou os endpoints de analytics não existem ainda no backend. Verificar:
```bash
grep -rn "IS_API_MODE" frontend/src/domains/analytics/
```

**Fix necessário:**
1. Auditar cada tab de Analytics: identificar quais endpoints existem no backend e quais ainda não foram implementados.
2. Para endpoints existentes: substituir mock por chamada real.
3. Para endpoints não implementados: exibir "—" ou estado vazio com mensagem "Dados não disponíveis ainda" — nunca dados inventados.
4. `ProductHealth` score (55, 95, 0, 70) — se calculado no frontend a partir de dados reais, manter lógica; se hardcoded, substituir.

**Critério de aceite:**
- [ ] Produto sem conteúdo → saúde de Conteúdo = 0%, não 95%.
- [ ] Produto sem forms → "Sem submissions", não "0 Formulários ativos com 439 respostas".
- [ ] Nenhuma tab de Analytics exibe dados que contradizem o estado real do produto.

---

## Seção F — Convites por produto (4 bugs críticos)

### F.1 — Convite via produto Conecta Talentos não dispara nenhuma ação, email não chega

**Sintoma:** Logado como Super Admin no contexto do produto "Conecta Talentos", acessar `/users/invite`, preencher form com email de teste, clicar "Enviar convite" → nenhuma ação visível, permanece na tela, MailHog não recebe email.

**Causa raiz provável:** O `InviteUserDrawer` passa `effectiveTenant.id` (CLIENTES BETA) para `usersService.invite()`, mas o produto "Conecta Talentos" pode não ter `ProductAssignment` criado no backend, ou a chamada `POST /tenants/{tenantId}/users/invite` está falhando silenciosamente sem exibir erro.

**Investigação necessária:**
1. Abrir DevTools → Network → verificar se o request de convite é enviado e qual status HTTP retorna.
2. Verificar se "Conecta Talentos" tem UUID válido no banco (produto pode ter sido criado apenas no frontend mock).
3. Verificar se `InviteUserDrawer` tem handler de erro que exibe toast em caso de falha.

**Fix necessário:**
1. Adicionar `catch` robusto no handler de envio:
   ```ts
   } catch (err) {
     const msg = isHttpError(err, 404) ? "Produto não encontrado."
               : isHttpError(err, 403) ? "Sem permissão para convidar."
               : "Erro ao enviar convite. Tente novamente.";
     toast.error(msg);
   }
   ```
2. Se o produto não existe no backend mas existe no frontend: criar seed ou migration para garantir que produtos criados via UI são persistidos.
3. Verificar `POST /tenants/{id}/users/invite` está mapeado para produto correto.

**Critério de aceite:**
- [ ] Convidar usuário para Conecta Talentos → email chega no MailHog em até 5 segundos.
- [ ] Falha na API → toast de erro descritivo (não silêncio).
- [ ] Usuário convidado aparece na tabela com produto "Conecta Talentos".

---

### F.2 — Email de convite mostra produto/tenant errado (Maestro Beton em vez de Conecta Talentos) — CRÍTICO

**Sintoma:** Convidar usuário para produto "Conecta Talentos" → email chega com "Produtos: Maestro Beton" no corpo. Falha grave de contexto.

**Causa raiz (confirmada em código):**
O `InviteUserDrawer` usa `effectiveProduct` para resolver qual produto está no contexto da sessão. Se o usuário navegou de "Maestro Beton" para "Conecta Talentos" mas `effectiveProduct` ainda aponta para "Maestro Beton" (produto padrão/último ativo), o convite é criado com o contexto errado.

Fluxo de dados:
```
InviteUserDrawer → usersService.invite({ allowedProducts: [selectedProductId] }, tenantId)
                → POST /tenants/{id}/users/invite
                → ProductAssignmentService.inviteUser(product = produto errado)
                → KeycloakProductAssignmentInvitePort.invite(... productName = "Maestro Beton")
                → IdentityActionInviteCommand(productNames = ["Maestro Beton"])
                → Email template recebe productNames = "Maestro Beton"
```

**Fix necessário:**
1. `InviteUserDrawer` deve usar o produto **selecionado no form** (`allowedProducts`), não `effectiveProduct` do contexto.
2. O campo "Produtos permitidos" do form deve passar o `productId` selecionado para o service.
3. O backend deve usar o `productId` recebido no request (não derivar do contexto) para buscar o nome do produto no email.
4. Verificar `KeycloakProductAssignmentInvitePort.invite()` — o `productName` passado vem do `Product` do banco, não da sessão? Confirmar.

**Fix frontend:**
```ts
// InviteUserDrawer.tsx
await usersService.invite({
  name,
  email,
  role,
  allowedProducts: selectedProductIds,  // ← IDs do form, não do contexto
  productId: selectedProductIds[0],       // ← produto principal do convite
}, effectiveTenant?.id);
```

**Critério de aceite:**
- [ ] Convidar para Conecta Talentos → email mostra "Produtos: Conecta Talentos".
- [ ] Convidar para Maestro Beton → email mostra "Produtos: Maestro Beton".
- [ ] Cabeçalho do email: tenant correto (CLIENTES BETA é o tenant, produto aparece no corpo).
- [ ] Bruno test: criar convite para produto X → validar email no MailHog com produto X.

---

### F.3 — Email diz "Você já tem uma conta!" para usuário que nunca se cadastrou

**Sintoma:** Email de convite para usuário de email nunca usado na plataforma exibe painel verde "Você já tem uma conta! Seu acesso ao produto Maestro Beton foi configurado. Faça login normalmente para começar." — mas o usuário não tem conta.

**Causa raiz:** H.1 do BUG-SPRINT-01 foi implementado (`hasRequiredAction(UPDATE_PASSWORD)`) mas pode estar retornando `false` mesmo para novos usuários. Hipóteses:

1. **Keycloak não adiciona `UPDATE_PASSWORD` automaticamente** ao criar usuário via `inviteByEmail()`. O Keycloak precisa ser configurado para adicionar essa required action no momento da criação do usuário.
2. **`KeycloakUserResponse.requiredActions`** pode ser `null` ou lista vazia para usuários criados sem a action explícita — e o código atual faz `return requiredActions != null && requiredActions.contains(action)` que retorna `false` → `requiresPasswordSetup = false` → frontend exibe "você já tem conta".

**Investigação necessária:**
```bash
# Verificar no Keycloak Admin se o usuário recém-criado tem UPDATE_PASSWORD
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:9090/admin/realms/aegis/users?email=novo@email.com | jq '.[0].requiredActions'
# Se retornar [] ou null: Keycloak não está adicionando a required action
```

**Fix necessário:**
1. Em `KeycloakAdminClient.inviteUser()` (ou onde o usuário é criado): ao criar o usuário, incluir `"requiredActions": ["UPDATE_PASSWORD"]` no body da criação.
2. Verificar `POST /admin/realms/{realm}/users` — o body já inclui `requiredActions`? Se não, adicionar.
3. Alternativa: após criar usuário, chamar `PUT /admin/realms/{realm}/users/{id}` com `{"requiredActions": ["UPDATE_PASSWORD"]}`.

**Fix backend (exemplo):**
```java
// KeycloakAdminClient — ao criar usuário via convite
Map<String, Object> userRepresentation = Map.of(
    "email", email,
    "enabled", true,
    "requiredActions", List.of("UPDATE_PASSWORD")  // ← garantir que está aqui
);
```

**Critério de aceite:**
- [ ] Convidar email nunca usado → email exibe formulário de criação de senha ("Bem-vindo, defina sua senha").
- [ ] Convidar email de usuário existente → email exibe "Você já tem uma conta!" corretamente.
- [ ] `hasRequiredAction(keycloakId, "UPDATE_PASSWORD")` retorna `true` para novo usuário.

---

### F.4 — Usuário convidado para Conecta Talentos aparece na tabela sem produto associado

**Sintoma:** Após aceitar o convite, o usuário "Alexandre Teste" aparece na `UserTable` do tenant com coluna "Produtos" vazia. Deveria exibir "Conecta Talentos".

**Causa raiz:** `UserTable` exibe os produtos do usuário buscando em `TenantMembership` mas o `ProductAssignment` para "Conecta Talentos" pode não ter sido criado, ou o DTO de `GET /tenants/{id}/users` não retorna os produtos associados ao usuário.

**Fix necessário:**
1. Verificar se `GET /tenants/{id}/users` retorna campo `products: [{ id, name }]` para cada usuário.
2. Verificar se o `ProductAssignment` foi criado no momento do convite (Fix B do BUG-SPRINT-01 deveria garantir isso).
3. Se `ProductAssignment` existe mas não aparece no DTO: adicionar o mapeamento no `TenantUserService.listUsers()`.
4. `UserTable` exibe a lista de produtos no campo "Produtos" — garantir que renderiza `assignment.productName` para cada assignment.

**Critério de aceite:**
- [ ] Convidar para Conecta Talentos → usuário aparece na tabela com "Conecta Talentos" na coluna Produtos.
- [ ] Usuário com múltiplos produtos → todos listados (separados por vírgula ou badges).
- [ ] Usuário sem produto associado (membro de tenant) → coluna Produtos exibe "—" ou "Apenas tenant".

---

## Seção G — Keycloak / Sessão (1 bug)

### G.1 — Timeout de sessão muito curto, usuário precisa relogar constantemente

**Sintoma:** Durante a bateria de testes, o usuário foi deslogado várias vezes por inatividade. O timeout está muito agressivo para uso em desenvolvimento e demonstração.

**Causa raiz:** Configuração do Keycloak realm (`SSO Session Idle`, `SSO Session Max`, `Access Token Lifespan`) com valores baixos.

**Fix necessário:**
1. Ajustar via Keycloak Admin (ou via `realm.json` de importação):
   - `SSO Session Idle`: 8 horas (dev) / 30 minutos (prod)
   - `SSO Session Max`: 24 horas (dev) / 8 horas (prod)
   - `Access Token Lifespan`: 30 minutos (dev) / 5 minutos (prod)
2. Se o ambiente usa `realm.json` para importação inicial, atualizar o arquivo em `infra/keycloak/`.
3. Documentar os valores recomendados por ambiente em `infra/keycloak/README.md`.

**Alternativa mais rápida (ajuste via variável de ambiente):**
```yaml
# docker-compose.yml ou .env
KEYCLOAK_SSO_SESSION_IDLE_TIMEOUT: 28800  # 8h em segundos
KEYCLOAK_ACCESS_TOKEN_LIFESPAN: 1800      # 30min em segundos
```

**Critério de aceite:**
- [ ] Sessão em ambiente local dura mínimo 4 horas sem relogar.
- [ ] Após ajuste, reiniciar Keycloak e confirmar que token não expira em 10 minutos.
- [ ] Documentar em `infra/keycloak/README.md`: timeouts configurados e como ajustar por ambiente.

---

## Seção H — Varredura preventiva (bugs encontrados na análise de código)

> O agente deve verificar todos os itens abaixo mesmo que o PO não tenha encontrado manualmente — são pontos de risco identificados na leitura do código durante esta sprint.

### H.1 — Rota de metadados do asset usa slug hardcoded

**Arquivo:** `frontend/src/domains/assets/pages/AssetDetail.tsx` — linha ~69
```tsx
<Button onClick={() => navigate("/assets/hero-maestro-beton/metadata")}>
  Editar metadados
</Button>
```
Hardcoded `hero-maestro-beton` — sempre abre os metadados do mesmo asset.

**Fix:** `navigate(`/assets/${assetId}/metadata`)` onde `assetId = useParams().assetId`.

---

### H.2 — `assetsService.getDownloadUrl` não verifica IS_API_MODE

```tsx
getDownloadUrl(assetId: string): string {
  return `${resolveBaseUrl()}/assets/${assetId}/download`;
}
```
Em modo mock, esta URL aponta para um endpoint que não existe no mock, causando 404. Deve retornar um blob URL ou ativar download do arquivo em memória.

**Fix:** Em modo mock, retornar `URL.createObjectURL(blob)` se o arquivo estiver em memória; ou exibir toast "Download disponível apenas em modo API".

---

### H.3 — `WorkflowBoard` importa tipos de `content.mocks` diretamente

```ts
// content/pages/WorkflowBoard.tsx — linha 12
import type { PendingDrop, WFEvent, WFItem, WFStatus } from "../mocks/content.mocks";
```
Tipos de domínio não devem vir do arquivo de mocks. Criar `content/contracts/types.ts` com esses tipos e importar de lá tanto nos mocks quanto no componente real.

---

### H.4 — `formsService.publishForm` com guard duplo frágil

```ts
if (IS_API_MODE && formId) return apiClient.post(...)
```
Se `formId` for `undefined` em modo API (bug na inicialização), o publish cai no mock silenciosamente. Substituir por:
```ts
if (!formId) throw new Error("formId obrigatório para publicar formulário");
if (IS_API_MODE) return apiClient.post(...);
```

---

### H.5 — `SettingsOverview` hardcoda múltiplos itens de "Atenção administrativa"

Além do item de "2 usuários com convite pendente" (D.3), verificar se outros itens como "1 integração sem configuração", "Permissões de Editor foram alteradas há 2 dias", "Auditoria possui eventos críticos recentes" também são hardcoded. Se sim, corrigir com chamadas reais ou remover.

---

### H.6 — InviteUserDrawer não muda effectiveProduct ao trocar de produto no form

Se o usuário seleciona "Conecta Talentos" no dropdown "Produtos permitidos" do form mas o `effectiveProduct` do `AuthContext` ainda é "Maestro Beton", o convite pode ser criado para o produto errado. Garantir que o `productId` enviado ao backend vem do form selecionado, nunca do `effectiveProduct` do contexto.

---

## Seção I — Critérios de aceite globais da sprint

### I.1 — Gates obrigatórios antes de merge

```bash
# 1. TypeScript
cd frontend && npm run typecheck
# Esperado: 0 erros

# 2. JaCoCo (novos endpoints)
cd backend && mvn verify -q
# Esperado: BUILD SUCCESS + jacoco-check PASSED

# 3. Bruno smoke tests
cd bruno && bru run --env local
# Esperado: 0 FAILED

# 4. Verificação manual de contexto de convite
# Convidar para produto A → email menciona produto A (não produto B)
# Convidar novo usuário → email mostra form de senha (não "já tem conta")
```

### I.2 — Checklist visual obrigatório

- [ ] AssetDetail: preview real para JPG, PDF, MP4
- [ ] Download: arquivo com nome correto e extensão
- [ ] Assets na modal AssetPicker: aparecem após upload rápido
- [ ] Dashboard editorial: KPIs = 0 para produto sem conteúdo
- [ ] Preview do workflow: conteúdo real com identidade Aegis
- [ ] Forms dashboard: 0 formulários para produto sem forms
- [ ] Configurações/Segurança: nenhuma menção a "Sprint 07"
- [ ] Filtros de Segurança: dinâmicos e funcionais
- [ ] SettingsOverview: convites pendentes reais
- [ ] Convite Conecta Talentos: email com produto correto
- [ ] Novo usuário convidado: email exige criação de senha
- [ ] Usuário na tabela: produto associado visível
- [ ] Sessão: dura 4+ horas sem relogar
- [ ] Nenhum banner "Erro parcial" nas telas principais

### I.3 — Novos endpoints criados nesta sprint

Para cada endpoint novo criado no backend, o agente deve:
1. Criar request Bruno em `bruno/` na collection correspondente
2. Criar teste de request com assertions de status e body
3. Garantir que o serviço/controller novo tem cobertura JaCoCo ≥ 100% de linhas e branches

---

## Seção J — Usuários (1 bug)

### J.1 — Filtro "Papel / status / produto" na tela de usuários não abre popover

**Sintoma:** Clicar no botão "Papel / status / produto" (ícone funil, canto superior direito da `UserTable`) exibe um elemento vazio/popup em branco, ou não abre nada. O filtro não funciona — aparentemente renderiza um container sem conteúdo.

**Evidência:** Print mostra um retângulo branco vazio aparecendo abaixo do botão ao clicar — estrutura do popover existe mas sem conteúdo interno.

**Causa raiz provável:** O componente de filtro (`FilterPanel` ou `Popover`) foi montado mas o conteúdo interno (chips de papel, status e produto) está renderizando como `null` ou está fora da viewport. Verificar em `UserTable.tsx` se o popover de filtros tem seu conteúdo condicional a algum estado que nunca é verdadeiro.

**Investigação necessária:**
1. Localizar em `frontend/src/domains/users/pages/UserTable.tsx` o handler do botão "Papel / status / produto".
2. Verificar se o `Popover`/`DropdownMenu`/`FilterPanel` tem conteúdo real ou está vazio.
3. Verificar se `tenantProducts`, `roles` e `statuses` usados para popular os chips estão sendo carregados antes do render.

**Fix necessário:**
1. O popover deve exibir três grupos de filtros com chips clicáveis:
   - **Papel:** Editor, Product Manager, Tenant Admin, Super Admin (+ "Todos")
   - **Status:** Ativo, Convidado, Inativo (+ "Todos")
   - **Produto:** lista dinâmica dos produtos do tenant (`tenantProducts`)
2. Ao selecionar um filtro, a tabela de usuários filtra localmente (sem nova requisição).
3. Múltiplos filtros simultâneos: AND lógico (papel = Editor AND status = ativo).
4. Botão "Limpar filtros" reseta todos os filtros ativos.
5. Identidade visual: **exatamente igual** ao `FilterPanel` implementado em `AuditTimeline` — mesmos chips, cores, ícones Lucide, comportamento de "Todos" e "Limpar".

**Fix de referência (padrão já implementado em AuditTimeline):**
```tsx
// UserTable.tsx — popover de filtros
const [filters, setFilters] = useState({ role: "Todos", status: "Todos", product: "Todos" });

const ROLE_OPTIONS   = ["Todos", "editor", "product_manager", "tenant_admin", "super_admin"];
const STATUS_OPTIONS = ["Todos", "ativo", "convidado", "inativo"];
const PRODUCT_OPTIONS = ["Todos", ...tenantProducts.map(p => p.name)];

// Aplicar filtros na lista:
const filtered = users.filter(u =>
  (filters.role    === "Todos" || u.role    === filters.role)    &&
  (filters.status  === "Todos" || u.status  === filters.status)  &&
  (filters.product === "Todos" || u.products.includes(filters.product))
);
```

**Critério de aceite:**
- [ ] Clicar "Papel / status / produto" → popover abre com 3 grupos de chips visíveis.
- [ ] Selecionar "editor" → tabela exibe apenas usuários com papel editor.
- [ ] Selecionar "convidado" → tabela exibe apenas convidados.
- [ ] Selecionar produto "Maestro Beton" → apenas usuários com esse produto.
- [ ] Combinar: role=editor + status=ativo → filtro AND.
- [ ] "Todos" num grupo → reseta apenas aquele grupo.
- [ ] "Limpar" → reseta todos os grupos.
- [ ] tsc --noEmit: zero erros.
- [ ] Visual idêntico ao FilterPanel do AuditTimeline.

---

## Apêndice — Mapa de mocks remanescentes identificados

| Arquivo | Linha | Dado hardcoded | Prioridade |
|---|---|---|---|
| `AssetDetail.tsx` | ~32 | `assetName = "hero-maestro-beton.jpg"` | Alta |
| `AssetDetail.tsx` | ~69 | rota `/assets/hero-maestro-beton/metadata` | Alta |
| `AssetPreviewPanel` | ~15-24 | badge "imagem", nome, dimensões, checksum | Alta |
| `SettingsOverview.tsx` | ~14 | `"2 usuários com convite pendente."` | Média |
| `SecuritySettingsPanel.tsx` | ~160 | texto "Integração mock — Sprint 07" | Alta |
| `EditorialDashboard.tsx` | KPIs | valores mock de rascunhos/publicados/etc | Alta |
| `FormsDashboard.tsx` | KPIs | 4 forms, 439 respostas, 7.4% conversão | Alta |
| `WorkflowBoard.tsx` | import | tipos importados de `content.mocks` | Baixa |
| `ContentEditor.tsx` | toast | "conflito de edição há 2 min" hardcoded | Média |
| `ResponsivePreviewFrame` | conteúdo | preview não renderiza body real | Alta |
