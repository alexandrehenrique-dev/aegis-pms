// Sprint 18, Tarefa C — preview de conteúdo e de formulário sempre legíveis,
// mesmo com o app em tema escuro. Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, openFirstPageEditor, collectPageErrors, report } from "./helpers.mjs";

/** Cor próxima de `#1d1d1f` (foreground claro) — tolera pequenas diferenças de arredondamento do navegador. */
function isLightForeground(rgbString) {
  const m = rgbString.match(/\d+/g)?.map(Number) ?? [];
  const [r, g, b] = m;
  return r < 60 && g < 60 && b < 60;
}

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    // Força o app inteiro (fora do preview) para tema escuro — é exatamente
    // o cenário do bug original: fundo do preview fixo em branco + texto
    // reagindo ao tema (dark:prose-invert / herança de `color`).
    await loginAndOpenProduct(page, { theme: "dark" });
    ok = report("App carregou em tema escuro", await page.evaluate(() => document.documentElement.classList.contains("dark"))) && ok;

    await openFirstPageEditor(page);
    await page.getByText("Preview", { exact: true }).click();
    await page.waitForTimeout(1200);

    const headingColor = await page.evaluate(() => {
      const scope = document.querySelector(".light");
      const h = scope ? [...scope.querySelectorAll("h1,h2,h3")].find((el) => el.textContent?.trim().length) : null;
      return h ? getComputedStyle(h).color : null;
    });
    ok = report("Preview de conteúdo: heading renderiza com cor escura (não branca/invertida)", headingColor && isLightForeground(headingColor), headingColor) && ok;

    const bg = await page.evaluate(() => {
      const box = document.querySelector(".light");
      return box ? getComputedStyle(box).backgroundColor : null;
    });
    ok = report("Wrapper `.light` do preview existe no DOM", !!bg, bg) && ok;

    // Preview de formulário (sem Markdown — testa só a herança de `color` via `.light`).
    await page.getByText("Voltar ao editor", { exact: true }).click().catch(() => {});
    await page.waitForTimeout(500);
    await page.getByText("Forms", { exact: true }).first().click();
    await page.waitForTimeout(800);
    await page.getByText("Ver formulários").click();
    await page.waitForTimeout(800);
    await page.getByText("Preview", { exact: true }).first().click();
    await page.waitForTimeout(800);
    const formTitleColor = await page.evaluate(() => {
      const h = [...document.querySelectorAll("h2")].find((el) => el.textContent?.includes("Contato"));
      return h ? getComputedStyle(h).color : null;
    });
    ok = report("Preview de formulário: título renderiza com cor escura", formTitleColor && isLightForeground(formTitleColor), formTitleColor) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
