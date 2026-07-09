// Domínio dashboard — ProductDashboard produto-first e DashboardGlobal administrativo.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, collectPageErrors, report, DEMO_USERS } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page, { user: DEMO_USERS.productManager, productName: "Maestro Beton" });
    ok = report("Product Manager entra no workspace operacional do produto", page.url().includes("/content")) && ok;
    ok = report("Workspace operacional mostra painel de Conteúdo", await page.getByText("Conteúdo", { exact: true }).first().isVisible()) && ok;
    ok = report("Product Manager nao cai no ProductDashboard administrativo", !page.url().includes("/products/")) && ok;
    ok = report("Product Manager nao ve Dashboard Global na sidebar", (await page.locator("aside").getByText("Dashboard", { exact: true }).count()) === 0) && ok;

    const adminPage = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
    const adminErrors = collectPageErrors(adminPage);
    await loginAndOpenProduct(adminPage, { user: DEMO_USERS.tenantAdmin, productName: "Maestro Beton" });
    await goToNav(adminPage, "Dashboard");
    ok = report("Tenant admin acessa /dashboard (Dashboard Global, tenant-wide)", adminPage.url().endsWith("/dashboard")) && ok;
    // "Produtos ativos" e "Financeiro" sao restritos a tenant_admin/super_admin (PermGate) — editor nao os ve.
    ok = report("Dashboard Global tem o KPI 'Conteúdos pendentes'", await adminPage.getByText("Conteúdos pendentes").isVisible()) && ok;
    ok = report("Tenant admin ve KPI administrativo 'Produtos ativos'", await adminPage.getByText("Produtos ativos").isVisible()) && ok;

    await adminPage.getByText("Conteúdos pendentes").click();
    await adminPage.waitForTimeout(800);
    ok = report("KPI 'Conteúdos pendentes' navega para /content/list", adminPage.url().endsWith("/content/list")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0 && adminErrors.length === 0, [...errors, ...adminErrors].join(" | ")) && ok;
    await adminPage.close();
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
