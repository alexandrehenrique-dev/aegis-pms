// Regressão — bug encontrado durante a verificação manual da Sprint 18 (não
// fazia parte do escopo original): digitar mais rápido que o debounce de
// persistência do PageEditor (CONTENT_SAVE_DEBOUNCE_MS = 500ms) perdia quase
// todos os caracteres de qualquer campo de bloco, porque o campo controlado
// só refletia o conteúdo depois do round-trip assíncrono do mock. Corrigido
// em domains/pages/pages/PageEditor.tsx (`handleChangeContent` agora aplica
// um `setPage` otimista a cada tecla). Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, openFirstPageEditor, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await openFirstPageEditor(page);

    const titleInput = page.locator('label:has-text("title") input').first();
    const original = await titleInput.inputValue();
    const expected = "Hello World Testing 123";

    // `.fill()` simula um valor inteiro chegando de uma vez (equivalente a
    // colar texto, ou a digitação rápida que expôs o bug original).
    await titleInput.fill(expected);
    await page.waitForTimeout(1500); // > CONTENT_SAVE_DEBOUNCE_MS, garante que o debounce já disparou.
    const afterFill = await titleInput.inputValue();
    ok = report("Campo controlado preserva o valor inteiro após fill() + debounce", afterFill === expected, `esperado="${expected}" obtido="${afterFill}"`) && ok;

    // Digitação caractere a caractere bem mais rápida que o debounce.
    await titleInput.fill(original);
    await page.waitForTimeout(800);
    await titleInput.fill("");
    await titleInput.type("abcdefghijklmnopqrstuvwxyz", { delay: 15 });
    await page.waitForTimeout(1500);
    const afterTyping = await titleInput.inputValue();
    ok = report("Digitação rápida (15ms/tecla) não perde caracteres", afterTyping === "abcdefghijklmnopqrstuvwxyz", `obtido="${afterTyping}"`) && ok;

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
