// Domínio content — dashboard editorial, lista (DataGrid), editor, workflow board, preview, versoes.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, goToTab, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await goToNav(page, "Conteúdo");
    ok = report("Dashboard editorial carrega", page.url().endsWith("/content")) && ok;

    await goToTab(page, "Lista");
    ok = report("Lista de conteudos (DataGrid) carrega", page.url().endsWith("/content/list")) && ok;

    await page.getByRole("button", { name: "Abrir" }).first().click();
    await page.waitForTimeout(1000);
    ok = report("Abrir um conteudo entra no editor", page.url().includes("/editor")) && ok;
    ok = report("Editor de conteudo mostra campo de titulo", await page.locator('label:has-text("Título") input').first().isVisible()) && ok;

    await page.goBack();
    await page.waitForTimeout(800);
    await page.getByRole("button", { name: "Preview" }).first().click();
    await page.waitForTimeout(1000);
    ok = report("Preview do conteudo abre", page.url().includes("/preview")) && ok;

    await page.goBack();
    await page.waitForTimeout(800);
    await page.getByRole("button", { name: "Histórico" }).first().click();
    await page.waitForTimeout(1000);
    ok = report("Historico de versoes abre", page.url().includes("/versions")) && ok;

    await goToNav(page, "Conteúdo");
    await goToTab(page, "Workflow");
    ok = report("Workflow board (Kanban) carrega", page.url().endsWith("/content/workflow")) && ok;
    ok = report("Board mostra colunas de status (Draft)", await page.getByText("Draft", { exact: true }).first().isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
