import { collectPageErrors, DEMO_USERS, launchBrowser, loginAndOpenProduct, report } from "./helpers.mjs";

const browser = await launchBrowser();
const page = await browser.newPage();
const errors = collectPageErrors(page);

try {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await loginAndOpenProduct(page, { user: DEMO_USERS.productManager, productName: "Loki" });
  await page.goto(`${process.env.AEGIS_BASE_URL ?? "http://localhost:5173"}/content`, { waitUntil: "networkidle" });
  await page.keyboard.press(process.platform === "darwin" ? "Meta+K" : "Control+K");
  await page.getByPlaceholder("Buscar produtos, conteúdo, assets, formulários...").waitFor({ timeout: 5000 });
  const palette = page.locator(".fixed.inset-0.z-50").first();
  const paletteText = await palette.textContent();

  const forbiddenTexts = [
    "Aion Logbook",
    "Eirene UI",
    "Genesis",
    "hero-maestro-beton.jpg",
    "release-institucional.pdf",
    "Página Home",
  ];
  const visibleForbidden = [];
  for (const text of forbiddenTexts) {
    if (paletteText?.includes(text)) {
      visibleForbidden.push(text);
    }
  }

  const requiredTexts = ["Loki", "Manifesto do Silêncio", "Vigília"];
  const missingRequired = [];
  for (const text of requiredTexts) {
    if (!paletteText?.includes(text)) {
      missingRequired.push(text);
    }
  }

  const failures = [
    ...visibleForbidden.map((text) => `vazou item fora do escopo: ${text}`),
    ...missingRequired.map((text) => `faltou item do escopo atual: ${text}`),
    ...errors,
  ];

  report("global-search-scope", failures.length === 0, failures.join("; "));
  if (failures.length > 0) process.exitCode = 1;
} finally {
  await browser.close();
}
