// Domínio knowledge graph — overview, canvas do grafo, busca de entidade, detalhe, orfas, insights.
// Modulo "Knowledge Graph" so vem habilitado por padrao em produtos tipo "Knowledge Base"
// (PRODUCT_TYPE_MODULE_DEFAULTS) — Maestro Beton (Site Institucional) nao tem; usa-se
// "Aegis Docs" (tenant "Aegis Labs"), unico produto Knowledge Base ativo (nao arquivado) dos mocks.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, goToTab, DEMO_USERS, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page, { user: DEMO_USERS.tenantAdmin, tenantName: "Aegis Labs", productName: "Aegis Docs" });
    await goToNav(page, "Knowledge Graph");
    ok = report("Overview do Knowledge Graph carrega", page.url().endsWith("/knowledge")) && ok;
    ok = report("Overview mostra contagem de entidades totais", await page.getByText("Entidades totais").isVisible()) && ok;

    await goToTab(page, "Graph");
    ok = report("Graph Canvas carrega", page.url().endsWith("/knowledge/graph")) && ok;

    await goToTab(page, "Busca");
    ok = report("Entity Search carrega", page.url().endsWith("/knowledge/search")) && ok;
    await page.locator('input[placeholder*="Buscar"]').fill("Home");
    await page.waitForTimeout(500);
    const resultCard = page.locator("div", { hasText: "Home" }).first();
    ok = report("Busca por 'Home' retorna algum resultado", await resultCard.isVisible()) && ok;

    await goToTab(page, "Órfãos");
    ok = report("Tabela de entidades orfas carrega", page.url().endsWith("/knowledge/orphans")) && ok;

    await goToTab(page, "Insights");
    ok = report("Knowledge Insights carrega", page.url().endsWith("/knowledge/insights")) && ok;
    await page.getByRole("button", { name: "Abrir entidade" }).first().click();
    await page.waitForTimeout(800);
    ok = report("Abrir entidade pelo insight entra no detalhe", page.url().includes("/knowledge/entities/")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
