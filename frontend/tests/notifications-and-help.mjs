// Domínio notifications + help — sino de notificacoes, criar notificacao (super_admin, do
// /select-tenant), Central de Ajuda (abre o FeedbackModal pelo link "Reportar um problema").
// O FeedbackModal em si (anexo real, persistencia) ja e testado a fundo em feedback-attachment.mjs (Sprint 18).
// Ver tests/README.md.
import { launchBrowser, collectPageErrors, report, DEMO_USERS, BASE_URL, goToNav } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
    await page.locator('input[type="email"]').fill(DEMO_USERS.superAdmin.email);
    await page.locator('input[type="password"]').fill(DEMO_USERS.superAdmin.password);
    await page.getByRole("button", { name: "Entrar" }).click();
    await page.waitForTimeout(1000);
    ok = report("super_admin cai na selecao de tenant", page.url().includes("/select-tenant")) && ok;
    ok = report("Tela de selecao de tenant tem botao 'Criar Notificação'", await page.getByRole("button", { name: "Criar Notificação" }).first().isVisible()) && ok;

    await page.getByRole("button", { name: "Criar Notificação" }).first().click();
    await page.waitForTimeout(500);
    await page.locator('label:has-text("Título") input').fill(`Notificacao de teste ${Date.now()}`);
    await page.getByRole("button", { name: "Editar texto" }).click();
    await page.waitForTimeout(500);
    await page.locator("textarea").first().fill("Corpo da notificacao de teste, gerado pelo smoke test.");
    await page.getByRole("button", { name: "Salvar texto" }).click();
    await page.waitForTimeout(300);
    ok = report("Modal de criar notificacao tem o botao de criar", await page.getByRole("button", { name: /Criar notificação/ }).isVisible()) && ok;
    await page.getByRole("button", { name: /Criar notificação/ }).click();
    await page.waitForTimeout(800);

    // Entra no produto pra ver o sino de notificacoes e a Central de Ajuda.
    await page.getByText("BYOP", { exact: true }).first().click();
    await page.waitForTimeout(800);
    await page.getByText("Maestro Beton", { exact: true }).first().click();
    await page.waitForTimeout(1000);
    await page.getByText("Entendi").click({ timeout: 2000 }).catch(() => {});

    await page.getByLabel("Notificações").click();
    await page.waitForTimeout(500);
    ok = report("Sino de notificacoes abre o painel", await page.getByText("Marcar lidas").first().isVisible()) && ok;
    await page.getByLabel("Notificações").click();

    // /help nao tem item de sidebar — chega-se pelo botao "?" (HelpCircle) do topo do AppShell, em toda tela.
    await page.locator('button[title="Central de Ajuda"]').click();
    await page.waitForTimeout(800);
    ok = report("Central de Ajuda abre", page.url().endsWith("/help")) && ok;
    await page.getByText("Reportar um problema").click();
    await page.waitForTimeout(500);
    ok = report("Link 'Reportar um problema' abre o FeedbackModal", await page.getByText("Reportar problema", { exact: true }).isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
