// Domínio settings — visao geral, produto, tenant, permissoes, seguranca.
// Usa super_admin: tenant_admin e bloqueado em /settings/security (roleBlockedRoutePrefixes).
// `/settings/roles` so e alcancavel a partir de AuditEventDetail ("Abrir recurso") — testado em audit.mjs.
// `/settings/access-preview` nao tem nenhum link de entrada na UI hoje (so a rota existe) — fora de
// escopo de um smoke test client-side (exigiria navegacao direta por URL, que perde a sessao mock).
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, goToTab, DEMO_USERS, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page, { user: DEMO_USERS.superAdmin, tenantName: "BYOP", productName: "Maestro Beton" });
    await goToNav(page, "Configurações");
    ok = report("Visao geral de configuracoes carrega", page.url().endsWith("/settings")) && ok;

    await goToTab(page, "Produto");
    ok = report("Configuracoes de produto carrega", page.url().endsWith("/settings/product")) && ok;

    await goToTab(page, "Tenant");
    ok = report("Configuracoes de tenant carrega", page.url().endsWith("/settings/tenant")) && ok;

    await goToTab(page, "Permissões");
    ok = report("Matriz de permissoes carrega", page.url().endsWith("/settings/permissions")) && ok;
    ok = report("Matriz mostra acao 'Salvar permissões'", await page.getByRole("button", { name: "Salvar permissões" }).isVisible()) && ok;

    await goToTab(page, "Segurança");
    ok = report("Painel de seguranca carrega (super_admin)", page.url().endsWith("/settings/security")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
