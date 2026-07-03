// Sprint 23 — Inbox de feedbacks (Super Admin) e Alertas por Telegram em SecuritySettingsPanel.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, goToTab, DEMO_USERS, collectPageErrors, report } from "./helpers.mjs";

async function submitFeedback(page, { category, priority, description }) {
  await page.getByText("Super", { exact: true }).click();
  await page.waitForTimeout(300);
  await page.getByText("Reportar problema").click();
  await page.waitForTimeout(500);
  await page.getByRole("button", { name: category, exact: true }).click();
  await page.getByRole("button", { name: priority, exact: true }).click();
  await page.locator("textarea").fill(description);
  await page.getByRole("button", { name: "Enviar feedback" }).click();
  await page.waitForTimeout(600);
  await page.getByRole("button", { name: "Fechar" }).click();
  await page.waitForTimeout(300);
}

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page, { user: DEMO_USERS.superAdmin, tenantName: "BYOP", productName: "Maestro Beton" });

    // Popula o store em memória (vazio a cada reload) com 2 feedbacks reais
    // antes de abrir a inbox, mesma técnica de tests/feedback-attachment.mjs.
    await submitFeedback(page, { category: "Bug", priority: "crítica", description: "Smoke test — botão não responde." });
    await submitFeedback(page, { category: "UX confusa", priority: "alta", description: "Smoke test — fluxo confuso no wizard." });
    // O toast "Feedback registrado · AGS-####" (duration 5000ms, FeedbackModal)
    // também bate no regex /AGS-\d{4}/ usado abaixo para contar linhas — espera
    // ele sumir em vez de escopar o locator, mais simples que introduzir um
    // seletor novo só para o teste.
    await page.waitForTimeout(5600);

    ok = report("Item 'Feedbacks' visível na sidebar do super_admin", await page.locator("aside").getByText("Feedbacks", { exact: true }).isVisible()) && ok;

    await goToNav(page, "Feedbacks");
    ok = report("Rota /admin/feedback carrega", page.url().endsWith("/admin/feedback")) && ok;
    ok = report("Lista mostra os 2 feedbacks recém-criados", (await page.getByText(/AGS-\d{4}/).count()) === 2) && ok;
    // Badge só é conferido depois da navegação: o count refaz fetch por
    // `location.pathname` (ver AppShell.tsx), não há pub/sub no store mock.
    ok = report("Badge de contagem 'aberto' aparece na sidebar", await page.locator("aside").getByText("2", { exact: true }).isVisible()) && ok;

    await page.getByRole("button", { name: "Abertos 2" }).click();
    await page.waitForTimeout(300);
    ok = report("Filtro 'Abertos' mantém os 2 itens (ambos nascem 'aberto')", (await page.getByText(/AGS-\d{4}/).count()) === 2) && ok;

    await page.getByRole("button", { name: "Resolvidos 0" }).click();
    await page.waitForTimeout(300);
    ok = report("Filtro 'Resolvidos' mostra o estado vazio", await page.getByText(/Nenhum feedback por aqui/).isVisible()) && ok;

    await page.getByRole("button", { name: "Todos", exact: true }).click();
    await page.waitForTimeout(300);

    await page.getByRole("button", { name: "crítica", exact: true }).click();
    await page.waitForTimeout(300);
    ok = report("Filtro de prioridade 'Crítica' reduz para 1 item", (await page.getByText(/AGS-\d{4}/).count()) === 1) && ok;
    await page.getByRole("button", { name: "crítica", exact: true }).click();
    await page.waitForTimeout(300);

    await page.locator('button[aria-label="Mais ações"]').first().click();
    await page.waitForTimeout(300);
    // ContextActionMenu renderiza as duas variantes (desktop/mobile) no DOM
    // ao mesmo tempo, só uma visível via CSS — `.first()` pega a de desktop
    // (ordem real no JSX), a única visível neste viewport 1600px.
    await page.getByText("Ver detalhes").first().click();
    await page.waitForTimeout(400);
    ok = report("Drawer de detalhe abre com a descrição do feedback", await page.getByText(/Smoke test/).first().isVisible()) && ok;

    const urlBeforeStatusChange = page.url();
    await page.getByRole("button", { name: "Marcar como resolvido" }).first().click();
    await page.waitForTimeout(500);
    ok = report("URL não muda ao trocar status (sem reload de página)", page.url() === urlBeforeStatusChange) && ok;
    ok = report("Badge muda para 'Resolvido' no drawer após a ação", await page.getByText("Resolvido", { exact: true }).isVisible()) && ok;

    await page.keyboard.press("Escape");
    await page.waitForTimeout(500);

    await goToNav(page, "Configurações");
    await goToTab(page, "Segurança");
    ok = report("Seção 'Alertas por Telegram' aparece em Segurança", await page.getByText("Alertas por Telegram").isVisible()) && ok;
    ok = report("Placeholder 'Telegram futuro' não existe mais", (await page.getByText("Telegram futuro").count()) === 0) && ok;

    await page.getByLabel("Chat ID").fill("-1001234567890");
    await page.getByLabel("Bot Token").fill("123456:ABC-DEF-fake-token-1234");
    await page.getByRole("button", { name: "Salvar", exact: true }).click();
    await page.waitForTimeout(500);
    ok = report("Toast de sucesso ao salvar Telegram", await page.getByText("Telegram configurado ✓").isVisible()) && ok;
    ok = report("Token nunca aparece em claro (só mascarado)", (await page.getByText("123456:ABC-DEF-fake-token-1234").count()) === 0) && ok;
    ok = report("Badge 'configurado' aparece após salvar", await page.getByText("configurado", { exact: true }).isVisible()) && ok;

    await page.getByRole("button", { name: "Remover configuração" }).click();
    await page.waitForTimeout(500);
    ok = report("Toast de sucesso ao remover Telegram", await page.getByText("Configuração do Telegram removida.").isVisible()) && ok;
    ok = report("Formulário volta ao estado sem configuração (sem badge)", (await page.getByText("configurado", { exact: true }).count()) === 0) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
