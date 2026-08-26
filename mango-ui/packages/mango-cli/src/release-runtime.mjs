import { readFileSync } from 'node:fs';

export function assertReleaseNodeVersion({ manifestPath, nodeVersion = process.versions.node }) {
  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  const expectedRange = manifest.engines?.node;
  const range = typeof expectedRange === 'string' ? /^>=(\d+)\.(\d+)\.(\d+) <(\d+)$/u.exec(expectedRange.trim()) : null;
  const current = /^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$/u.exec(nodeVersion);
  if (!range) throw new Error('Mango release Node engine must use the governed >=x.y.z <major range');
  if (!current) throw new Error(`Mango release cannot parse the current Node version: ${nodeVersion}`);

  const minimum = range.slice(1, 4).map(Number);
  const upperMajor = Number(range[4]);
  const observed = current.slice(1, 4).map(Number);
  if (compareVersions(observed, minimum) < 0 || observed[0] >= upperMajor) {
    throw new Error(`Mango release requires Node ${expectedRange}; current Node is ${nodeVersion}`);
  }
  return { expectedRange, nodeVersion };
}

function compareVersions(left, right) {
  for (let index = 0; index < left.length; index += 1) {
    if (left[index] !== right[index]) return left[index] - right[index];
  }
  return 0;
}
