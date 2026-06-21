// Sprint 18, Tarefa B — cor de texto na toolbar do markdown + sanitização no cliente.
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, openFirstPageEditor, addBlock, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await openFirstPageEditor(page);
    await addBlock(page, "text");

    await page.getByText("Editar texto").click();
    await page.waitForTimeout(500);
    const textarea = page.locator("textarea").first();

    // Paleta fechada de cor (Tarefa B.1) — selecionar tudo e aplicar "Vermelho".
    await textarea.fill("Texto colorido de teste");
    await page.keyboard.press("Meta+A"); // Cmd+A no Chromium/macOS; Ctrl+A move o cursor em vez de selecionar.
    await page.locator('button[title="Cor do texto"]').click();
    await page.waitForTimeout(300);
    const colorTitles = ["Vermelho", "Azul", "Verde", "Âmbar", "Violeta", "Padrão (remover cor)"];
    let allColorsVisible = true;
    for (const t of colorTitles) allColorsVisible = allColorsVisible && (await page.locator(`button[title="${t}"]`).isVisible());
    ok = report("Popover mostra as 6 opções fixas (5 cores + Padrão)", allColorsVisible) && ok;
    await page.locator('button[title="Vermelho"]').click();
    await page.waitForTimeout(300);
    const wrapped = await textarea.inputValue();
    ok = report("Texto selecionado é envolvido em <span class=\"text-aegis-red\">", wrapped === '<span class="text-aegis-red">Texto colorido de teste</span>', wrapped) && ok;

    // Sanitização no cliente (Tarefa B.2) — cola payload malicioso e confere o HTML renderizado.
    await textarea.fill(
      '<span class="text-aegis-blue">Azul ok</span> e <script>alert(1)</script> <img src=x onerror="alert(2)"> <span style="color:red">style bloqueado</span>',
    );
    await page.getByText("Salvar texto").click();
    await page.waitForTimeout(800);
    const renderedHtml = await page.locator(".prose").first().innerHTML();
    ok = report("Classe de cor permitida (span.text-aegis-blue) passa", renderedHtml.includes('<span class="text-aegis-blue">Azul ok</span>'), renderedHtml) && ok;
    ok = report("<script> é removido (tag e conteúdo)", !renderedHtml.includes("alert(1)") && !renderedHtml.includes("<script"));
    ok = report("<img onerror> é removido", !renderedHtml.includes("<img"));
    ok = report("atributo style é removido do span (span sobrevive sem ele)", renderedHtml.includes("style bloqueado") && !renderedHtml.includes("style="));

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
