// Domínio audit — timeline, detalhe de evento, link cruzado dinâmico por módulo do evento.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, DEMO_USERS, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    // /audit so aparece na sidebar para super_admin/tenant_admin (roleVisibleNav).
    await loginAndOpenProduct(page, { user: DEMO_USERS.tenantAdmin, tenantName: "BYOP", productName: "Maestro Beton" });
    await goToNav(page, "Auditoria");
    ok = report("Audit Timeline carrega", page.url().endsWith("/audit")) && ok;
    ok = report("Auditoria renderiza tabela paginada", await page.getByRole("table").isVisible()) && ok;
    ok = report("Filtros de auditoria ficam em toolbar", await page.getByPlaceholder("Ator, evento, alvo...").isVisible()) && ok;
    ok = report("Paginação da auditoria aparece", await page.getByRole("button", { name: /Próxima/ }).isVisible()) && ok;

    // G.3 (BUG-SPRINT-05) — AuditEventDetail agora busca o evento real via
    // useParams + auditService.getEvent, e "Abrir recurso" navega por
    // event.module (resourceRoute) em vez de sempre /settings/roles fixo.
    // O 2º card do mock ("Ana Martins · permissão alterada", módulo
    // "Permissions") é o único cujo módulo mapeia para /settings/roles.
    await page.getByRole("button", { name: "Abrir" }).nth(1).click();
    await page.waitForTimeout(800);
    ok = report("Ver detalhe entra no evento", page.url().includes("/audit/")) && ok;

    await page.getByRole("button", { name: "Abrir recurso" }).click();
    await page.waitForTimeout(800);
    ok = report("'Abrir recurso' do evento de Permissions leva a /settings/roles", page.url().endsWith("/settings/roles")) && ok;
    ok = report("Gestao de papeis (Role Management) carrega", await page.locator("body").isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
