// Domínio assets — biblioteca, detalhe de asset, tags, upload real de arquivo.
// Ver tests/README.md.
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { launchBrowser, loginAndOpenProduct, goToNav, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await goToNav(page, "Assets");
    ok = report("Biblioteca de assets carrega", page.url().endsWith("/assets")) && ok;

    await page.getByRole("button", { name: "Abrir" }).first().click();
    await page.waitForTimeout(800);
    ok = report("Abrir asset entra no detalhe", page.url().match(/\/assets\/[^/]+$/) !== null) && ok;
    ok = report("Detalhe do asset mostra acao 'Baixar'", await page.getByRole("button", { name: "Baixar" }).isVisible()) && ok;

    await goToNav(page, "Assets");
    await page.getByRole("button", { name: "Upload de asset" }).click();
    await page.waitForTimeout(800);
    ok = report("Tela de upload abre com input[type=file] real", (await page.locator('input[type="file"]').count()) >= 1) && ok;

    const tmpFile = path.join(os.tmpdir(), `aegis-asset-test-${Date.now()}.png`);
    fs.writeFileSync(tmpFile, Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]));
    await page.locator('input[type="file"]').setInputFiles(tmpFile);
    await page.waitForTimeout(500);
    await page.getByRole("button", { name: "Concluir upload" }).click();
    await page.waitForTimeout(1000);
    ok = report("Upload concluido volta pra biblioteca com toast de sucesso", page.url().endsWith("/assets")) && ok;
    fs.unlinkSync(tmpFile);

    // Tags
    await page.getByRole("button", { name: "Organizar tags" }).click();
    await page.waitForTimeout(800);
    ok = report("Tela de tags carrega", page.url().endsWith("/assets/tags")) && ok;
    await page.getByRole("button", { name: "Criar tag", exact: true }).click();
    await page.waitForTimeout(300);
    const tagName = `tag-teste-${Date.now()}`;
    await page.locator('label:has-text("Nome") input, label:has-text("Tag") input').first().fill(tagName).catch(async () => {
      await page.locator("input").last().fill(tagName);
    });
    await page.getByRole("button", { name: "Criar", exact: true }).click();
    await page.waitForTimeout(800);
    ok = report("Tag criada aparece na lista", await page.getByText(tagName).first().isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
