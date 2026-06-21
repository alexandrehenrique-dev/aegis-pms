// Domínio dashboard — ProductDashboard (landing pós-login) e DashboardGlobal (tenant-wide, via sidebar).
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    ok = report("Landing pos-login e o ProductDashboard do produto selecionado", page.url().includes("/products/")) && ok;
    ok = report("ProductDashboard mostra KPIs operacionais (Status do produto)", await page.getByText("Status do produto").isVisible()) && ok;
    ok = report("ProductDashboard lista modulos do produto (Conteúdo)", await page.getByText("Operar páginas, artigos e workflow editorial.").isVisible()) && ok;

    await goToNav(page, "Dashboard");
    ok = report("Sidebar 'Dashboard' navega para /dashboard (Dashboard Global, tenant-wide)", page.url().endsWith("/dashboard")) && ok;
    // "Produtos ativos" e "Financeiro" sao restritos a tenant_admin/super_admin (PermGate) — editor nao os ve.
    ok = report("Dashboard Global tem o KPI 'Conteúdos pendentes'", await page.getByText("Conteúdos pendentes").isVisible()) && ok;
    ok = report("KPI restrito a outros papeis ('Produtos ativos') fica oculto para editor", (await page.getByText("Produtos ativos").count()) === 0) && ok;

    await page.getByText("Conteúdos pendentes").click();
    await page.waitForTimeout(800);
    ok = report("KPI 'Conteúdos pendentes' navega para /content/list", page.url().endsWith("/content/list")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
