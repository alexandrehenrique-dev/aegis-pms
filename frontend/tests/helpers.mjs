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

/** Captura erros de runtime do app (console.error/pageerror) — chame no início do teste e confira o array vazio no final. */
export function collectPageErrors(page) {
  const errors = [];
  page.on("pageerror", (err) => errors.push(`pageerror: ${err.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });
  return errors;
}

/**
 * Login + seleção de tenant/produto (fluxo Aegis PMS) até cair no dashboard
 * do produto "Maestro Beton". `theme` opcional ("light"/"dark") seta
 * `localStorage.aegis-theme` antes do login, para testar telas que reagem
 * ao tema do app (ex.: Tarefa C da Sprint 18 — preview "papel branco").
 */
export async function loginAndOpenProduct(page, { user = DEMO_USERS.editor, theme } = {}) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
  if (theme) {
    await page.evaluate((t) => localStorage.setItem("aegis-theme", t), theme);
    await page.reload({ waitUntil: "networkidle" });
  }
  await page.locator('input[type="email"]').fill(user.email);
  await page.locator('input[type="password"]').fill(user.password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await page.waitForTimeout(800);
  await page.getByText("Entrar →").click();
  await page.waitForTimeout(800);
  await page.getByText("Abrir →").click();
  await page.waitForTimeout(1000);
  // Modal de boas-vindas só aparece no primeiro acesso da sessão — ignora se não existir.
  await page.getByText("Entendi").click({ timeout: 2000 }).catch(() => {});
  await page.waitForTimeout(500);
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
