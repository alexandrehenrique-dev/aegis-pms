// Sprint 18, Tarefa A — blocos `video`/`video-gallery`.
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

    await addBlock(page, "video");
    ok = report("Bloco `video` renderiza o editor dedicado", await page.getByText("Fonte do vídeo").isVisible()) && ok;
    ok = report("Fonte padrão é upload (botão 'Selecionar vídeo')", await page.getByText("Selecionar vídeo").isVisible()) && ok;

    // Asset picker filtrado por "vídeo" (Tarefa A.4)
    await page.getByText("Selecionar vídeo").click();
    await page.waitForTimeout(500);
    const videoBadgeClass = (await page.getByText("vídeo", { exact: true }).first().getAttribute("class")) ?? "";
    ok = report("Filtro 'vídeo' do picker de assets vem pré-selecionado", videoBadgeClass.includes("ede9fe")) && ok;
    ok = report("Asset de vídeo existente do mock aparece na lista", await page.getByText("depoimento-video.mp4").isVisible()) && ok;
    await page.getByText("Cancelar", { exact: true }).click();
    await page.waitForTimeout(300);

    // Troca para YouTube e valida URL (Tarefa A.7)
    await page.locator('button:has-text("upload")').click();
    await page.waitForTimeout(200);
    await page.getByRole("menuitem", { name: "youtube", exact: true }).click();
    await page.waitForTimeout(500);
    const urlInput = page.locator('label:has-text("URL do YouTube") input');
    await urlInput.fill("https://vimeo.com/12345");
    await page.waitForTimeout(400);
    ok = report("URL fora do padrão mostra mensagem de erro", await page.getByText("URL do YouTube inválida").isVisible()) && ok;
    await urlInput.fill("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
    await page.waitForTimeout(400);
    ok = report("URL válida some com a mensagem de erro", !(await page.getByText("URL do YouTube inválida").isVisible())) && ok;

    // Catálogo de blocos inclui video-gallery (Tarefa A.1/A.5)
    await addBlock(page, "video-gallery");
    ok = report("Bloco `video-gallery` adiciona com sucesso", await page.getByText("Adicionar item").isVisible()) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
