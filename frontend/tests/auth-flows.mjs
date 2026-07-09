// Domínio auth — login (válido/inválido/bloqueado), seleção de tenant/produto, logout, esqueci minha senha.
// Ver tests/README.md.
import { launchBrowser, collectPageErrors, report, DEMO_USERS, BASE_URL, suppressTutorialAutostart } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
    await suppressTutorialAutostart(page);

    // Credenciais invalidas
    // `handleLogin` tem um delay artificial de 700ms (simula latencia de rede) antes de resolver o erro.
    await page.locator('input[type="email"]').fill("editor@byop.io");
    await page.locator('input[type="password"]').fill("senha-errada");
    await page.getByRole("button", { name: "Mostrar senha" }).click();
    ok = report("Olho da senha revela a senha sem navegar para esqueci senha", await page.locator('input[type="text"]').isVisible() && page.url().includes("/login")) && ok;
    await page.getByRole("button", { name: "Ocultar senha" }).click();
    ok = report("Olho da senha volta a ocultar a senha ainda em /login", await page.locator('input[type="password"]').isVisible() && page.url().includes("/login")) && ok;
    await page.getByRole("button", { name: "Entrar" }).click();
    await page.waitForTimeout(1000);
    ok = report("Senha errada mostra mensagem de credenciais invalidas", await page.getByText("E-mail ou senha incorretos").isVisible()) && ok;

    // Conta bloqueada
    await page.locator('input[type="email"]').fill("blocked@byop.io");
    await page.locator('input[type="password"]').fill("senha123");
    await page.getByRole("button", { name: "Entrar" }).click();
    await page.waitForTimeout(1000);
    ok = report("Conta bloqueada mostra mensagem especifica", await page.getByText("Esta conta está bloqueada").isVisible()) && ok;

    // Esqueci minha senha
    await page.getByText("Esqueci minha senha").click();
    await page.waitForTimeout(500);
    ok = report("Tela 'Esqueci minha senha' abre", page.url().includes("/forgot-password")) && ok;
    await page.goBack();
    await page.waitForTimeout(500);

    // Login valido (editor) -> produto -> dashboard. PM/editor/viewer seguem
    // jornada produto-first: o tenant é contexto técnico, não uma escolha que
    // esses perfis precisam fazer.
    await page.locator('input[type="email"]').fill(DEMO_USERS.editor.email);
    await page.locator('input[type="password"]').fill(DEMO_USERS.editor.password);
    await page.getByRole("button", { name: "Entrar" }).click();
    await page.waitForTimeout(1000);
    ok = report("Apos login valido, editor nao cai na selecao de tenant", !page.url().includes("/select-tenant")) && ok;
    ok = report("Apos login valido, editor cai na selecao de produto", page.url().includes("/select-product")) && ok;
    await page.getByText("Maestro Beton", { exact: true }).first().click();
    await page.waitForTimeout(1000);
    await page.getByText("Entendi").click({ timeout: 2000 }).catch(() => {});
    ok = report("Apos selecionar produto, editor cai no workspace operacional do produto", page.url().includes("/content")) && ok;

    // Logout
    await page.getByText("Rafael", { exact: true }).click();
    await page.waitForTimeout(300);
    await page.getByText("Sair da plataforma", { exact: true }).click();
    await page.waitForTimeout(500);
    ok = report("Logout volta para /login", page.url().includes("/login")) && ok;

    // Convite (rota publica, alcancavel sem sessao)
    await page.goto(`${BASE_URL}/invite`, { waitUntil: "networkidle" });
    ok = report("Tela de convite (/invite) carrega sem sessao", await page.locator("body").isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
