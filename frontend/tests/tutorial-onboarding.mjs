// Tutorial interativo de onboarding (Sprint 22 —
// docs/sprints/frontend/22_tutorial_de_onboarding_interativo.md).
// Cobre: checkbox pré-marcado no modal de boas-vindas, o tour iniciando
// automaticamente, gate por papel (um Product Manager nunca vê os steps de
// "criar produto"/"gerenciar usuários"/"zona de perigo"), "Pular tutorial"
// marcando como concluído, e a garantia de "nunca mostrar novamente" após
// reload. Usa `pm@byop.io` (product_manager) de propósito: nenhum outro
// script de `tests/` loga com esse usuário, então a notificação
// ONBOARDING_WELCOME nunca foi mostrada/dispensada por outro script — sem
// isso, `getPendingModal()` não devolveria a notificação numa execução via
// `run-all.mjs` (ver "Limitações conhecidas" em tests/README.md).
import { launchBrowser, collectPageErrors, report, DEMO_USERS, BASE_URL } from "./helpers.mjs";

// Títulos gerados a partir de `core/tutorial/tutorialSteps.ts` — mantidos em
// sincronia manualmente, mesmo texto (Seção G do documento da sprint).
const ROLE_GATED_OUT_FOR_PM = ["Seu painel de comando", "Criar um produto digital", "Novos produtos em segundos", "Sua equipe", "Convidar alguém é simples", "Zona de perigo"];
const EXPECTED_FOR_PM = ["Seja bem-vindo ao Aegis", "Configurações do workspace", "Você está pronto"];

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
    // Garante "nunca completou o tutorial" independente de execuções anteriores no mesmo navegador.
    await page.evaluate(() => localStorage.removeItem("aegis:tutorial:completed"));
    await page.locator('input[type="email"]').fill(DEMO_USERS.productManager.email);
    await page.locator('input[type="password"]').fill(DEMO_USERS.productManager.password);
    await page.getByRole("button", { name: "Entrar" }).click();
    await page.waitForTimeout(800);
    ok = report("Product Manager entra direto na selecao de produto", page.url().includes("/select-product")) && ok;
    // Product Manager pode ver vários produtos autorizados — escolhe um explícito.
    await page.getByText("Maestro Beton", { exact: true }).first().click();
    await page.waitForTimeout(1200);

    const checkbox = page.getByRole("checkbox");
    ok = report("Checkbox 'Fazer um tour pela plataforma' aparece no modal de boas-vindas", await checkbox.isVisible()) && ok;
    ok = report("Checkbox vem pré-marcado", await checkbox.isChecked()) && ok;

    await page.getByText("Entendi").click();
    await page.waitForTimeout(600);

    let tooltip = page.getByRole("alertdialog");
    ok = report("Tour inicia automaticamente ~300ms após fechar o modal", (await tooltip.textContent())?.includes("Seja bem-vindo ao Aegis")) && ok;

    // Percorre o tour clicando "Próximo" até o botão virar "Concluir",
    // registrando o título de cada step efetivamente mostrado (alguns são
    // pulados por papel/módulo — ver TARGET_NOT_FOUND em TutorialContext.tsx)
    // e checando que o tooltip fica inteiro dentro da viewport — alvos muito
    // grandes (ex.: a tabela de conteúdo inteira) já empurraram o tooltip
    // pra fora da tela, sem nenhum botão alcançável (bug real encontrado em
    // produto no /content/list — corrigido ancorando no <thead>, não mais
    // na tabela inteira; ver tutorialSteps.ts).
    const seenTitles = [];
    const viewport = page.viewportSize();
    for (let i = 0; i < 20; i++) {
      tooltip = page.getByRole("alertdialog");
      const text = (await tooltip.textContent().catch(() => null)) ?? "";
      if (text) seenTitles.push(text);
      const box = await tooltip.boundingBox().catch(() => null);
      if (box) {
        const withinBounds = box.x >= 0 && box.y >= 0 && box.x + box.width <= viewport.width && box.y + box.height <= viewport.height;
        ok = report(`Tooltip do step "${text.slice(0, 30)}..." fica inteiro dentro da viewport`, withinBounds, JSON.stringify(box)) && ok;
      }
      const lastButton = tooltip.getByRole("button", { name: "Concluir" });
      if (await lastButton.isVisible().catch(() => false)) {
        await lastButton.click();
        break;
      }
      const nextButton = tooltip.getByRole("button", { name: /^Próximo/ });
      if (!(await nextButton.isVisible().catch(() => false))) break;
      await nextButton.click();
      // Alguns steps pulam por TARGET_NOT_FOUND (ver TutorialContext.tsx):
      // aguarda o targetWaitTimeout (1000ms) do react-joyride + navegação de
      // rota + re-render antes do próximo tooltip aparecer.
      await page.waitForTimeout(1500);
    }
    await page.waitForTimeout(500);

    for (const gatedTitle of ROLE_GATED_OUT_FOR_PM) {
      ok = report(`Step "${gatedTitle}" nunca aparece para product_manager`, !seenTitles.some((t) => t.includes(gatedTitle))) && ok;
    }
    for (const expectedTitle of EXPECTED_FOR_PM) {
      ok = report(`Step "${expectedTitle}" aparece para product_manager`, seenTitles.some((t) => t.includes(expectedTitle))) && ok;
    }

    ok = report("Tour encerrado após 'Concluir' (overlay não fica na tela)", !(await page.getByRole("alertdialog").isVisible().catch(() => false))) && ok;

    await page.reload({ waitUntil: "networkidle" });
    await page.waitForTimeout(1000);
    ok = report("Após reload, tour não reaparece (localStorage marcado como concluído)", !(await page.getByRole("alertdialog").isVisible().catch(() => false))) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo do tutorial", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
