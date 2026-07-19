import fs from 'node:fs';
import path from 'node:path';

function toPosix(value) {
  return value.split(path.sep).join('/');
}

export function discoverTypecheckTargets(uiRoot) {
  const targets = [];
  const skipped = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const directory = path.join(parent, entry.name);
      const manifestFile = path.join(directory, 'package.json');
      if (!fs.existsSync(manifestFile)) continue;
      const manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));
      const tsconfig = path.join(directory, 'tsconfig.json');
      if (!fs.existsSync(tsconfig)) {
        skipped.push({
          workspace: manifest.name,
          directory: toPosix(path.relative(uiRoot, directory)),
          reason: 'missing-tsconfig',
        });
        continue;
      }
      targets.push({ workspace: manifest.name, directory, tsconfig });
    }
  }
  return {
    targets: targets.sort((left, right) => left.workspace.localeCompare(right.workspace)),
    skipped: skipped.sort((left, right) => left.workspace.localeCompare(right.workspace)),
  };
}

export function parseTypeScriptDiagnostics(output, uiRoot, workspace) {
  const diagnostics = [];
  const expression = /^(.*?)\((\d+),(\d+)\):\s+(error|warning)\s+(TS\d+):\s+(.+)$/gmu;
  for (const match of output.matchAll(expression)) {
    const absolute = path.isAbsolute(match[1]) ? match[1] : path.resolve(uiRoot, match[1]);
    diagnostics.push({
      workspace,
      file: toPosix(path.relative(uiRoot, absolute)),
      line: Number(match[2]),
      column: Number(match[3]),
      severity: match[4],
      code: match[5],
      message: match[6].trim(),
    });
  }
  return diagnostics;
}
