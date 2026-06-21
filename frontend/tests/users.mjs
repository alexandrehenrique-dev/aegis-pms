// Domínio users — lista, convite, detalhe. `/users` nao tem item de sidebar (so super_admin/tenant_admin
// alcancam via "Próximas ações" do ProductDashboard, QuickActions.tsx + roleActions). Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, DEMO_USERS, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page, { user: DEMO_USERS.superAdmin, tenantName: "BYOP", productName: "Maestro Beton" });
    // super_admin cai em /dashboard — chega no ProductDashboard (onde mora "Próximas ações") via /products.
    await page.getByText("Produtos ativos").click();
    await page.waitForTimeout(800);
    await page.locator('input[placeholder*="Buscar"]').fill("Maestro");
    await page.waitForTimeout(300);
    await page.getByRole("button", { name: /^Abrir/ }).click();
    await page.waitForTimeout(800);

    await page.getByText("Monitorar usuários e convites").click();
    await page.waitForTimeout(800);
    ok = report("Lista de usuarios carrega via Proximas acoes", page.url().endsWith("/users")) && ok;

    await page.getByRole("button", { name: "Convidar" }).click();
    await page.waitForTimeout(800);
    ok = report("Tela de convite de usuario abre", page.url().endsWith("/users/invite")) && ok;
    ok = report("Convite mostra campos de papel/produtos", await page.getByText("Convidar Usuário").isVisible()) && ok;

    await page.goBack();
    await page.waitForTimeout(800);
    await page.getByRole("button", { name: "Abrir" }).first().click();
    await page.waitForTimeout(800);
    ok = report("Abrir um usuario entra no detalhe", page.url().includes("/users/")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
