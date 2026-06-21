// Domínio analytics — overview e as 6 sub-abas (Saúde, Conteúdo, Forms, Canais, Relatórios, Tendências).
// Ver tests/README.md.
import { launchBrowser, loginAndOpenProduct, goToNav, goToTab, collectPageErrors, report } from "./helpers.mjs";

const TABS = [
  ["Saúde", "/analytics/health"],
  ["Conteúdo", "/analytics/content"],
  ["Forms", "/analytics/forms"],
  ["Canais", "/analytics/channels"],
  ["Relatórios", "/analytics/reports"],
  ["Tendências", "/analytics/trends"],
];

async function main() {
  const browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });
  const errors = collectPageErrors(page);
  let ok = true;

  try {
    await loginAndOpenProduct(page);
    await goToNav(page, "Analytics");
    ok = report("Overview de analytics carrega", page.url().endsWith("/analytics")) && ok;

    for (const [label, path] of TABS) {
      await goToTab(page, label);
      ok = report(`Aba '${label}' carrega (${path})`, page.url().endsWith(path)) && ok;
    }

    ok = report("Nenhum erro de console/runtime durante o fluxo", errors.length === 0, errors.join(" | ")) && ok;
  } finally {
    await browser.close();
  }

  process.exitCode = ok ? 0 : 1;
}

main();
