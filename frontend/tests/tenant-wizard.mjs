// Domínio tenants — wizard "Criar Tenant -> Criar Produto -> Atribuir Usuário"
// (CreateTenantWizardModal, aberto do /select-tenant como super_admin).
// Cobre 3 gaps fechados como follow-up da Sprint 19 (docs/sprints/frontend/19_escopo_super_admin_e_validacoes.md):
// plano do tenant editável, unicidade de slug de produto validada via
// productsService.checkSlugAvailable no blur, e validação XOR
// (userId vs inviteEmail) na atribuição de usuário ao produto.
// Ver tests/README.md.
import { launchBrowser, collectPageErrors, report, DEMO_USERS, BASE_URL } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
    await page.locator('input[type="email"]').fill(DEMO_USERS.superAdmin.email);
    await page.locator('input[type="password"]').fill(DEMO_USERS.superAdmin.password);
    await page.getByRole("button", { name: "Entrar" }).click();
    await page.waitForTimeout(1000);
    ok = report("super_admin cai na selecao de tenant", page.url().includes("/select-tenant")) && ok;

    await page.getByRole("button", { name: "Criar Tenant" }).first().click();
    await page.waitForTimeout(500);
    ok = report("Wizard de criacao de tenant abre", await page.getByRole("heading", { name: "Criar Tenant" }).isVisible()) && ok;

    // --- Passo 1: plano do tenant editavel (gap fechado apos a Sprint 19) ---
    ok = report("Campo 'Plano' visivel com default 'Starter'", await page.locator('label:has-text("Plano")').getByText("Starter").isVisible()) && ok;
    await page.locator('label:has-text("Plano") button').click();
    await page.waitForTimeout(200);
    await page.getByRole("menuitem", { name: "Enterprise" }).click();
    await page.waitForTimeout(200);
    ok = report("Selecionar 'Enterprise' atualiza o campo Plano", await page.locator('label:has-text("Plano")').getByText("Enterprise").isVisible()) && ok;

    const suffix = Date.now();
    await page.locator('label:has-text("Nome do tenant") input').fill(`TEST-E2E Tenant ${suffix}`);
    await page.locator('label:has-text("E-mail do Tenant Admin inicial") input').fill(`admin-e2e-${suffix}@byop.io`);
    await page.getByRole("button", { name: "Criar tenant", exact: true }).click();
    await page.waitForTimeout(800);
    ok = report("Avanca para o passo 2 (criar produto)", await page.getByText("Nome do produto").isVisible()) && ok;

    // --- Passo 2: unicidade de slug de produto via API no blur (gap fechado apos a Sprint 19) ---
    await page.locator('label:has-text("Nome do produto") input').fill("Maestro Beton");
    await page.locator('label:has-text("Slug") input').click();
    await page.locator('label:has-text("Slug") input').blur();
    await page.waitForTimeout(500);
    ok = report("Slug duplicado ('maestro-beton') mostra erro inline no blur", await page.getByText("já está em uso").isVisible()) && ok;
    ok = report("Botao 'Criar produto' fica desabilitado com slug duplicado", await page.getByRole("button", { name: "Criar produto" }).isDisabled()) && ok;

    await page.locator('label:has-text("Nome do produto") input').fill(`Produto E2E ${suffix}`);
    await page.locator('label:has-text("Slug") input').click();
    await page.locator('label:has-text("Slug") input').blur();
    await page.waitForTimeout(500);
    ok = report("Slug unico nao mostra erro de duplicidade", !(await page.getByText("já está em uso").isVisible())) && ok;

    await page.getByRole("button", { name: "Criar produto" }).click();
    await page.waitForTimeout(800);
    ok = report("Avanca para o passo 3 (atribuir usuario)", await page.getByText("Convidar novo").isVisible()) && ok;

    // --- Passo 3: validacao XOR userId/inviteEmail (gap fechado apos a Sprint 19) ---
    await page.getByRole("button", { name: "Convidar novo" }).click();
    await page.locator('label:has-text("Email") input').fill("convidado-e2e@byop.io");
    await page.waitForTimeout(200);

    await page.getByRole("button", { name: "Usuário existente" }).click();
    await page.waitForTimeout(300);
    await page.locator("div.max-h-40 button").first().click();
    await page.waitForTimeout(300);
    ok = report("Selecionar usuario existente com convite ainda preenchido mostra erro XOR", await page.getByText("não os dois").isVisible()) && ok;
    ok = report("Botao 'Concluir' fica desabilitado em estado XOR invalido (ambos preenchidos)", await page.getByRole("button", { name: "Concluir" }).isDisabled()) && ok;

    await page.getByRole("button", { name: "Convidar novo" }).click();
    await page.locator('label:has-text("Email") input').fill("");
    await page.getByRole("button", { name: "Usuário existente" }).click();
    await page.waitForTimeout(300);
    ok = report("Erro XOR some ao limpar o campo conflitante", !(await page.getByText("não os dois").isVisible())) && ok;
    ok = report("Botao 'Concluir' reabilita com apenas usuario existente selecionado", await page.getByRole("button", { name: "Concluir" }).isEnabled()) && ok;

    await page.getByRole("button", { name: "Concluir" }).click();
    await page.waitForTimeout(1000);
    ok = report("Wizard conclui e mostra o resumo (passo 4)", await page.getByText("Ir para o Produto").isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
