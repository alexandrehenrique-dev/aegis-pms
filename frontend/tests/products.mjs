// Domínio products — lista, criar produto, ProductDashboard, módulos.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, DEMO_USERS, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    // tenant_admin ve "Criar produto" (canCreate); editor nao (PermGate em ProductsList/CreateProductForm).
    // tenant_admin tem varios produtos no tenant BYOP — precisa escolher um, senao "Abrir ->" colide em varios cards.
    // getPostLoginLandingPath: tenant_admin/super_admin caem em /dashboard (hub do tenant), nao no produto direto.
    await loginAndOpenProduct(page, { user: DEMO_USERS.tenantAdmin, tenantName: "BYOP", productName: "Maestro Beton" });
    ok = report("tenant_admin cai no Dashboard Global (hub do tenant) pos-login", page.url().endsWith("/dashboard")) && ok;

    // Navegacao client-side (page.goto faria reload e perderia a sessao mock) — KPI "Produtos ativos" leva a /products.
    await page.getByText("Produtos ativos").click();
    await page.waitForTimeout(800);
    ok = report("Lista de produtos carrega (Maestro Beton visivel)", await page.getByText("Maestro Beton").first().isVisible()) && ok;

    await page.getByRole("button", { name: "Novo produto" }).click();
    await page.waitForTimeout(800);
    ok = report("Tela 'Novo produto' abre", page.url().includes("/products/new")) && ok;

    const productName = `Produto Teste ${Date.now()}`;
    await page.locator('label:has-text("Nome do produto") input').fill(productName);
    await page.waitForTimeout(300);
    ok = report("Preview do esqueleto de paginas aparece ao escolher um tipo", await page.getByText(/nasce/).first().isVisible()) && ok;

    await page.getByRole("button", { name: "Criar produto" }).click();
    await page.waitForTimeout(1200);
    ok = report("Criar produto mostra toast de sucesso", await page.getByText("Produto criado com sucesso").isVisible()) && ok;

    // A tela pode voltar automaticamente depois do create; se ainda houver
    // "Cancelar", usa o fluxo de UI, senao recarrega a lista mantendo a sessao.
    const cancelButton = page.getByRole("button", { name: "Cancelar" });
    if (await cancelButton.isVisible().catch(() => false)) {
      await cancelButton.click();
      await page.waitForTimeout(800);
    } else if (!page.url().endsWith("/products")) {
      await page.locator("nav").getByRole("button", { name: "Produtos" }).click();
      await page.waitForTimeout(800);
    }

    // Abre o ProductDashboard de Maestro Beton e ve Modulos — filtra a busca pra so restar 1 card "Abrir".
    await page.locator('input[placeholder*="Buscar"]').fill("Maestro");
    await page.waitForTimeout(300);
    await page.getByRole("button", { name: /^Abrir/ }).click();
    await page.waitForTimeout(800);
    await page.getByRole("button", { name: "Ver módulos" }).click();
    await page.waitForTimeout(800);
    ok = report("Pagina de modulos do produto carrega", page.url().includes("/modules")) && ok;

    // Bug fix Sprint 20 (Tarefa C) — habilitar um modulo "desabilitado" que
    // Maestro Beton ainda nao tem (Integracoes, ver products.mocks.ts) deve
    // propagar pro modulesList do produto e refletir na contagem do card.
    await page.locator("div.rounded-2xl", { hasText: "Integrações" }).getByRole("button", { name: "Habilitar" }).click();
    await page.waitForTimeout(800);
    ok = report("Modulo 'Integrações' mostra toast de habilitado", await page.getByText("Módulo habilitado!").isVisible()) && ok;

    await page.goBack();
    await page.waitForTimeout(800);
    await page.goBack();
    await page.waitForTimeout(800);
    await page.locator('input[placeholder*="Buscar"]').fill("Maestro");
    await page.waitForTimeout(300);
    ok = report("Contagem de modulos do card propagou (6 -> 7)", await page.getByText("7 módulos").isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
