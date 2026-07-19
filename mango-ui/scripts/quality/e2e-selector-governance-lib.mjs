import fs from 'node:fs';
import path from 'node:path';

const FORBIDDEN = [
  { rule: 'element-plus-internal-class', pattern: /\.el-[a-z0-9_-]+/gu },
  { rule: 'positional-nth', pattern: /\.nth\s*\(/gu },
  { rule: 'fixed-timeout', pattern: /\.waitForTimeout\s*\(/gu },
  { rule: 'forced-click', pattern: /force\s*:\s*true/gu },
];

export function analyzeE2eSpecSource(source, file = '<source>') {
  const violations = [];
  for (const { rule, pattern } of FORBIDDEN) {
    for (const match of source.matchAll(pattern)) {
      const before = source.slice(0, match.index);
      violations.push({
        rule,
        file,
        line: before.split(/\r?\n/u).length,
        evidence: match[0],
      });
    }
  }
  return violations;
}

export function analyzeE2eSpecs(specRoot) {
  const violations = [];
  for (const file of listFiles(specRoot)) {
    if (!/\.spec\.[cm]?[jt]sx?$/u.test(file)) continue;
    const relative = path.relative(specRoot, file).split(path.sep).join('/');
    violations.push(...analyzeE2eSpecSource(fs.readFileSync(file, 'utf8'), relative));
  }
  return violations;
}

function listFiles(root) {
  if (!fs.existsSync(root)) return [];
  return fs.readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
    const target = path.join(root, entry.name);
    return entry.isDirectory() ? listFiles(target) : [target];
  });
}
