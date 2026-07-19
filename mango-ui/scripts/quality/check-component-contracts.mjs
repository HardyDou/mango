#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import {
  compareLegacyComponentBaselines,
  readComponentContractRecords,
  validateComponentContracts,
} from './component-contracts-lib.mjs';
import { createFrontendInventory } from './frontend-inventory-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const baseRef =
  process.argv.find((argument) => argument.startsWith('--base-ref='))?.slice('--base-ref='.length) ||
  process.env.MANGO_COMPONENT_CONTRACT_BASE_REF ||
  'origin/main';

function readBaseRecords() {
  const commit = spawnSync('git', ['rev-parse', '--verify', `${baseRef}^{commit}`], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  if (commit.status !== 0) throw new Error(`Component contract base ref is unavailable: ${baseRef}`);
  const tree = spawnSync('git', ['ls-tree', '-r', '--name-only', baseRef, '--', 'mango-ui/apps', 'mango-ui/packages'], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  if (tree.status !== 0) throw new Error(`Cannot inspect component contract base tree: ${baseRef}`);
  const registryPaths = tree.stdout
    .split('\n')
    .filter((file) => /^mango-ui\/(?:apps|packages)\/[^/]+\/component-contracts\.json$/u.test(file));
  const baseRecords = [];
  for (const registryPath of registryPaths) {
    const result = spawnSync('git', ['show', `${baseRef}:${registryPath}`], {
      cwd: repositoryRoot,
      encoding: 'utf8',
    });
    if (result.status !== 0) throw new Error(`Cannot read base component registry: ${registryPath}`);
    baseRecords.push({ registry: JSON.parse(result.stdout) });
  }
  return baseRecords;
}

try {
  const inventory = createFrontendInventory(uiRoot);
  const records = readComponentContractRecords(uiRoot);
  const failures = validateComponentContracts(inventory, records);
  const baseRecords = readBaseRecords();
  if (baseRecords.length === 0) {
    console.log(`component contract ratchet BOOTSTRAP registries are not present at ${baseRef}`);
  } else {
    failures.push(...compareLegacyComponentBaselines(records, baseRecords));
  }
  if (failures.length > 0) throw new Error(failures.join('\n'));
  const componentCount = records.reduce((sum, record) => sum + record.registry.components.length, 0);
  const legacyCount = records.reduce((sum, record) => sum + record.registry.legacyComponentExports.length, 0);
  fs.mkdirSync(path.join(repositoryRoot, '.runtime/frontend-quality'), { recursive: true });
  console.log(
    `component contracts PASS ${JSON.stringify({ registryCount: records.length, componentCount, legacyCount, publicVueExportCount: inventory.summary.publicVueExportCount })}`,
  );
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
}
