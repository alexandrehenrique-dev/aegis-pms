// Sprint 18, Tarefa D — anexo real de arquivo no FeedbackModal + persistência via feedbackService.
// Ver tests/README.md.
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { launchBrowser, loginAndOpenProduct, collectPageErrors, report } from "./helpers.mjs";

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  const feedbackCalls = [];
  page.on("console", async (msg) => {
    const args = msg.args();
    if (args.length < 2) return;
    const head = await args[0].jsonValue().catch(() => null);
    if (typeof head === "string" && head.includes("/api/v1/feedback")) {
      feedbackCalls.push(await args[1].jsonValue().catch(() => null));
    }
  });
  let ok = true;

  try {
    await loginAndOpenProduct(page);

    await page.getByText("Rafael", { exact: true }).click();
    await page.waitForTimeout(300);
    await page.getByText("Reportar problema").click();
    await page.waitForTimeout(500);

    ok = report("Modal abre com input[type=file] real (não botão fake)", (await page.locator('input[type="file"]').count()) === 1) && ok;

    const tmpFile = path.join(os.tmpdir(), `aegis-feedback-test-${Date.now()}.png`);
    fs.writeFileSync(tmpFile, Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]));
    await page.locator('input[type="file"]').setInputFiles(tmpFile);
    await page.waitForTimeout(800);
    ok = report("Após escolher o arquivo, o nome aparece no botão de anexo", await page.getByText(path.basename(tmpFile)).isVisible()) && ok;

    await page.locator("textarea").fill("Teste automatizado — feedback com anexo real.");
    await page.getByRole("button", { name: "Enviar feedback" }).click();
    await page.waitForTimeout(1000);

    ok = report("Tela de sucesso mostra um id legível AGS-####", await page.getByText(/AGS-\d{4}/).first().isVisible()) && ok;

    const createCall = feedbackCalls.find((c) => c && typeof c === "object");
    ok = report("POST /api/v1/feedback foi chamado com attachmentAssetId preenchido", !!createCall?.attachmentAssetId, JSON.stringify(createCall)) && ok;

    fs.unlinkSync(tmpFile);
    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
