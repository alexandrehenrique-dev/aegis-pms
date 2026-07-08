// Domínio forms — dashboard, lista, builder, preview, submissions, publicacao, analytics.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, goToTab, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await goToNav(page, "Forms");
    ok = report("Dashboard de forms carrega", page.url().endsWith("/forms")) && ok;

    await page.getByRole("button", { name: "Ver formulários" }).click();
    await page.waitForTimeout(800);
    ok = report("Lista de formularios carrega", page.url().endsWith("/forms/list")) && ok;

    await page.getByRole("button", { name: "Editar" }).first().click();
    await page.waitForTimeout(800);
    ok = report("Editar formulario abre o builder", page.url().match(/\/forms\/[^/]+$/) !== null) && ok;
    ok = report("Builder mostra preview do botao Enviar", await page.getByRole("button", { name: "Botão Enviar" }).isVisible()) && ok;

    // Bug fix Sprint 20 (Tarefa D.1) — salvar rascunho/publicar deve mudar a
    // coluna "Publicação" do formulário na lista, não só logar a chamada.
    await page.getByRole("button", { name: "Salvar rascunho" }).click();
    await page.waitForTimeout(800);
    await goToNav(page, "Forms");
    await page.getByRole("button", { name: "Ver formulários" }).click();
    await page.waitForTimeout(800);
    ok = report("Salvar rascunho propagou 'Rascunho' na coluna Publicação", await page.getByRole("row").filter({ hasText: "Contato Comercial" }).getByText("Rascunho").isVisible()) && ok;

    await page.getByRole("button", { name: "Editar" }).first().click();
    await page.waitForTimeout(800);
    await page.getByRole("button", { name: "Publicar" }).click();
    await page.waitForTimeout(800);
    await goToNav(page, "Forms");
    await page.getByRole("button", { name: "Ver formulários" }).click();
    await page.waitForTimeout(800);
    ok = report("Publicar propagou 'Publicado' de volta na coluna Publicação", await page.getByRole("row").filter({ hasText: "Contato Comercial" }).getByText("Publicado").isVisible()) && ok;

    await page.getByRole("button", { name: "Editar" }).first().click();
    await page.waitForTimeout(800);
    await page.getByRole("button", { name: "Preview", exact: true }).click();
    await page.waitForTimeout(800);
    // H.3.3 (BUG-SPRINT-05) — preview agora carrega o form real via `/forms/:formId/preview`, nunca mais a rota generica `/forms/preview` sem `formId`.
    ok = report("Preview do formulario abre com o form real", page.url().match(/\/forms\/[^/]+\/preview$/) !== null) && ok;

    await goToNav(page, "Forms");
    await goToTab(page, "Submissions");
    ok = report("Tabela de submissions carrega", page.url().endsWith("/forms/submissions")) && ok;
    await page.getByRole("button", { name: "Abrir" }).first().click();
    await page.waitForTimeout(800);
    // H.1.2 — rota corrigida para /forms/:formId/submissions/:id (nunca mais /forms/submissions/1 hardcoded).
    ok = report("Abrir submission entra no detalhe", page.url().match(/\/forms\/[^/]+\/submissions\/[^/]+$/) !== null) && ok;

    await goToNav(page, "Forms");
    await goToTab(page, "Publicação");
    ok = report("Painel de publicacao carrega", page.url().endsWith("/forms/publication")) && ok;
    ok = report("Publicacao mostra o embed do formulario", await page.getByText(/aegis-form/).first().isVisible()) && ok;

    await goToNav(page, "Forms");
    await goToTab(page, "Analytics");
    ok = report("Analytics basico de forms carrega", page.url().endsWith("/forms/analytics")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
