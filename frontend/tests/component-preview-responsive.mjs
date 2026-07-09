import fs from "node:fs/promises";
import { collectPageErrors, DEMO_USERS, launchBrowser, loginAndOpenProduct, report } from "./helpers.mjs";

const SCREENSHOT_DIR = "/tmp/aegis-pms-component-preview";

const VIEWPORTS = [
  { name: "desktop", width: 1440, height: 1100 },
  { name: "tablet", width: 834, height: 1112 },
  { name: "mobile", width: 390, height: 844 },
];

const REQUIRED_TEXTS = [
  "Preview completo de componentes",
  "Galeria de mídia",
  "Vídeos em destaque",
  "Agenda",
  "Perguntas frequentes",
  "Fale com a equipe",
  "Baixar apresentação",
  "Ouça o resumo",
  "Siga o projeto",
];

const browser = await launchBrowser();
const page = await browser.newPage();
const errors = collectPageErrors(page);

try {
  await fs.mkdir(SCREENSHOT_DIR, { recursive: true });
  await page.setViewportSize({ width: 1440, height: 1100 });
  await loginAndOpenProduct(page, { user: DEMO_USERS.editor, productName: "Maestro Beton" });

  const failures = [];
  for (const viewport of VIEWPORTS) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.goto(`${process.env.AEGIS_BASE_URL ?? "http://localhost:5173"}/content/componentes/preview`, {
      waitUntil: "networkidle",
    });

    for (const text of REQUIRED_TEXTS) {
      await page.getByText(text, { exact: false }).first().waitFor({ timeout: 5000 });
    }

    const hasHorizontalOverflow = await page.evaluate(() => {
      const root = document.documentElement;
      return root.scrollWidth > window.innerWidth + 2;
    });
    if (hasHorizontalOverflow) {
      failures.push(`${viewport.name}: overflow horizontal`);
    }

    await page.screenshot({
      path: `${SCREENSHOT_DIR}/component-preview-${viewport.name}.png`,
      fullPage: true,
    });
  }

  if (errors.length > 0) {
    failures.push(...errors);
  }

  report("component-preview-responsive", failures.length === 0, failures.join("; "));
  if (failures.length > 0) {
    process.exitCode = 1;
  }
} finally {
  await browser.close();
}
