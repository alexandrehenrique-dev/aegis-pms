// Runner agregador — roda todo *.mjs de tests/ (exceto helpers.mjs e a si
// mesmo) como processo filho, contra um dev server já no ar, e devolve exit
// code 1 se qualquer um falhar. Pensado pra ser o unico ponto de entrada que
// um pipeline/CI chama: `node tests/run-all.mjs`. Ver "Integrar num
// pipeline/CI" em tests/README.md.
import { spawn } from "node:child_process";
import { readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SKIP = new Set(["helpers.mjs", "run-all.mjs"]);

const scripts = readdirSync(__dirname)
  .filter((f) => f.endsWith(".mjs") && !SKIP.has(f))
  .sort();

function runOne(script) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [path.join(__dirname, script)], { stdio: "inherit" });
    child.on("exit", (code) => resolve(code ?? 1));
  });
}

async function main() {
  console.log(`Rodando ${scripts.length} scripts contra ${process.env.AEGIS_BASE_URL ?? "http://localhost:5173"}...\n`);
  const results = [];
  for (const script of scripts) {
    console.log(`\n=== ${script} ===`);
    const code = await runOne(script);
    results.push({ script, code });
  }

  console.log("\n=== Resumo ===");
  let failed = 0;
  for (const { script, code } of results) {
    console.log(`${code === 0 ? "✅" : "❌"} ${script}`);
    if (code !== 0) failed++;
  }
  console.log(`\n${results.length - failed}/${results.length} scripts passaram.`);

  process.exitCode = failed > 0 ? 1 : 0;
}

main();
