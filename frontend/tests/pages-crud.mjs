// Domínio pages — lista, criar pagina, editor (remover bloco com confirmacao), globals (navbar/footer).
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, addBlock, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await goToNav(page, "Páginas");
    ok = report("Lista de paginas carrega (Home visivel)", await page.getByText("Home", { exact: true }).first().isVisible()) && ok;

    // Cria pagina nova
    await page.getByRole("button", { name: "Nova página" }).click();
    await page.waitForTimeout(500);
    const pageTitle = `Pagina Teste ${Date.now()}`;
    await page.locator('label:has-text("Título") input').fill(pageTitle);
    await page.waitForTimeout(300);
    await page.getByRole("button", { name: "Criar página" }).click();
    await page.waitForTimeout(1000);
    ok = report("Criar pagina nova abre o editor dela", page.url().includes("/editor")) && ok;

    // Adiciona e remove um bloco (confirmacao de exclusao)
    await addBlock(page, "text");
    await page.getByRole("button", { name: "Remover bloco" }).click();
    await page.waitForTimeout(500);
    ok = report("Remover bloco pede confirmacao", await page.getByText(/irreversível/).isVisible()) && ok;
    await page.getByRole("button", { name: "Confirmar", exact: true }).click();
    await page.waitForTimeout(800);

    // Globais (navbar/footer/redes sociais)
    await goToNav(page, "Páginas");
    await page.getByRole("button", { name: "Navbar, footer e redes sociais" }).click();
    await page.waitForTimeout(800);
    ok = report("Tela de globais (navbar/footer) carrega", page.url().includes("/products/globals")) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
