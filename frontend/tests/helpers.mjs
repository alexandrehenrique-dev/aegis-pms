// Helpers compartilhados pelos scripts de smoke test manuais em tests/.
// Ver tests/README.md para quando e como rodar cada script.
import { chromium } from "playwright";

export const BASE_URL = process.env.AEGIS_BASE_URL ?? "http://localhost:5173";

export const DEMO_USERS = {
  superAdmin: { email: "super-admin@byop.io", password: "senha123" },
  tenantAdmin: { email: "admin@byop.io", password: "senha123" },
  productManager: { email: "pm@byop.io", password: "senha123" },
  editor: { email: "editor@byop.io", password: "senha123" },
  viewer: { email: "viewer@byop.io", password: "senha123" },
};

export async function launchBrowser() {
  return chromium.launch();
}

/**
 * Ruído conhecido de bibliotecas (não é bug do app) — filtrado para não dar
 * falso negativo em todo teste que abre um `Dialog` do shadcn/Radix:
 * `react-remove-scroll`@2.7.2 + `@radix-ui/react-slot`@1.1.2 emitem
 * "Function components cannot be given refs" via `SlotClone`/`RemoveScroll`
 * internamente (upstream, dentro de node_modules) em React 18.3, mesmo
 * quando o Dialog funciona corretamente — confirmado funcionalmente em
 * `tests/assets.mjs` (criar tag) e recorrente em qualquer outro `Dialog`
 * (`RoleManagement`, `SecuritySettingsPanel`, `AccessPreviewPanel`,
 * `SubmissionTable`/`SubmissionDetails`). Corrigir exigiria patch de
 * node_modules ou upgrade de dependência — fora de escopo para um warning
 * cosmético de dev-mode sem efeito funcional.
 */
const KNOWN_LIBRARY_NOISE = [/Function components cannot be given refs/];

/** Captura erros de runtime do app (console.error/pageerror) — chame no início do teste e confira o array vazio no final. */
export function collectPageErrors(page) {
  const errors = [];
  page.on("pageerror", (err) => errors.push(`pageerror: ${err.message}`));
  page.on("console", (msg) => {
    if (msg.type() !== "error") return;
    const text = msg.text();
    if (KNOWN_LIBRARY_NOISE.some((p) => p.test(text))) return;
    errors.push(`console.error: ${text}`);
  });
  return errors;
}

/**
 * Login + seleção de tenant/produto (fluxo Aegis PMS). Usuários com mais de
 * um tenant/produto (super-admin, tenant-admin) caem na tela de seleção com
 * vários cards — passe `tenantName`/`productName` para escolher um
 * específico (clica no texto do nome, que propaga pro `onClick` do card por
 * bubbling); sem eles, assume o caso comum de 1 card só ("Entrar →"/"Abrir
 * →", como editor/viewer/product_manager veem para o tenant BYOP). `theme`
 * opcional ("light"/"dark") seta `localStorage.aegis-theme` antes do login,
 * para testar telas que reagem ao tema do app (ex.: Tarefa C da Sprint 18 —
 * preview "papel branco").
 */
export async function loginAndOpenProduct(page, { user = DEMO_USERS.editor, theme, tenantName, productName } = {}) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
  if (theme) {
    await page.evaluate((t) => localStorage.setItem("aegis-theme", t), theme);
    await page.reload({ waitUntil: "networkidle" });
  }
  await page.locator('input[type="email"]').fill(user.email);
  await page.locator('input[type="password"]').fill(user.password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await page.waitForTimeout(800);
  if (tenantName) await page.getByText(tenantName, { exact: true }).first().click();
  else await page.getByText("Entrar →").click();
  await page.waitForTimeout(800);
  if (productName) await page.getByText(productName, { exact: true }).first().click();
  else await page.getByText("Abrir →").click();
  await page.waitForTimeout(1000);
  // Modal de boas-vindas só aparece no primeiro acesso da sessão — ignora se não existir.
  await page.getByText("Entendi").click({ timeout: 2000 }).catch(() => {});
  await page.waitForTimeout(500);
}

/** Clica num item da sidebar principal (`app/layouts/navConfig.ts`) — escopado a `<aside>` pra não colidir com abas de módulo (mesmo texto, ex. "Dashboard") nem breadcrumb. */
export async function goToNav(page, label) {
  await page.locator("aside").getByText(label, { exact: true }).click();
  await page.waitForTimeout(800);
}

/** Clica numa aba de módulo (`app/layouts/navConfig.ts`, `moduleTabs`) — a tira logo abaixo do breadcrumb, ex. "Lista"/"Workflow" em Conteúdo. */
export async function goToTab(page, label) {
  await page.locator("div.overflow-x-auto").getByText(label, { exact: true }).click();
  await page.waitForTimeout(800);
}

/** Abre o editor da primeira página listada em "Páginas" (Home, no mock). */
export async function openFirstPageEditor(page) {
  await page.getByText("Páginas", { exact: true }).first().click();
  await page.waitForTimeout(800);
  await page.getByText("Editar", { exact: true }).first().click();
  await page.waitForTimeout(1200);
}

/**
 * Adiciona um bloco do `blockType` informado via dropdown "Estrutura" +
 * "Adicionar bloco". Localiza o trigger do dropdown pela posição (irmão do
 * botão "Adicionar bloco"), não pelo texto exibido — o texto do trigger é o
 * último tipo escolhido, então fixar "text" quebraria a partir do segundo
 * bloco adicionado na mesma sessão de página.
 */
export async function addBlock(page, blockType) {
  const addBlockButton = page.getByText("Adicionar bloco");
  const trigger = addBlockButton.locator("xpath=..").locator("button").first();
  await trigger.click();
  await page.waitForTimeout(200);
  await page.getByRole("menuitem", { name: blockType, exact: true }).click();
  await page.waitForTimeout(200);
  await addBlockButton.click();
  await page.waitForTimeout(800);
}

/** Resultado de um script de teste — usado para decidir o exit code do processo. */
export function report(name, passed, detail = "") {
  const icon = passed ? "✅" : "❌";
  console.log(`${icon} ${name}${detail ? ` — ${detail}` : ""}`);
  return passed;
}
