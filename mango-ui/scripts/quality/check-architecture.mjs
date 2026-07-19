#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { analyzeArchitecture, assertArchitecture } from './architecture-graph-lib.mjs';
import { compareArchitectureBaselines, compareWildcardSourceExports } from './architecture-ratchet-lib.mjs';

const currentFile = fileURLToPath(import.meta.url);
const uiRoot = path.resolve(path.dirname(currentFile), '../..');
const outputFile = path.resolve(uiRoot, '../.runtime/frontend-quality/architecture.json');
const baselineFile = path.join(uiRoot, 'architecture-exceptions.json');
const baseRef =
  process.argv.find((argument) => argument.startsWith('--base-ref='))?.slice('--base-ref='.length) ||
  process.env.MANGO_ARCHITECTURE_BASE_REF ||
  'origin/main';

function assertBaselineRatchet() {
  const repositoryRoot = path.resolve(uiRoot, '..');
  const baseCommit = spawnSync('git', ['rev-parse', '--verify', `${baseRef}^{commit}`], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  if (baseCommit.status !== 0) {
    throw new Error(`Architecture base ref is unavailable: ${baseRef}`);
  }
  const currentManifests = ['apps', 'packages'].flatMap((workspaceKind) => {
    const workspaceRoot = path.join(uiRoot, workspaceKind);
    return fs
      .readdirSync(workspaceRoot, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .map((entry) => path.join(workspaceRoot, entry.name, 'package.json'))
      .filter((manifestPath) => fs.existsSync(manifestPath))
      .map((manifestPath) => ({
        manifestPath,
        manifest: JSON.parse(fs.readFileSync(manifestPath, 'utf8')),
      }));
  });
  const currentWildcardManifests = currentManifests.filter(({ manifest }) =>
    Object.values(manifest.mangoArchitecture?.sourceExports || {}).some((config) =>
      (config.sourcePattern || config.source || '').includes('*'),
    ),
  );
  const baseManifests = currentWildcardManifests.flatMap(({ manifestPath }) => {
    const relativeManifest = path.relative(repositoryRoot, manifestPath).split('\\').join('/');
    const baseManifest = spawnSync('git', ['show', `${baseRef}:${relativeManifest}`], {
      cwd: repositoryRoot,
      encoding: 'utf8',
    });
    return baseManifest.status === 0 ? [JSON.parse(baseManifest.stdout)] : [];
  });
  const wildcardFailures = compareWildcardSourceExports(
    currentWildcardManifests.map(({ manifest }) => manifest),
    baseManifests,
  );
  if (wildcardFailures.length > 0) {
    throw new Error(
      `Wildcard source exports may only stay equal or shrink relative to ${baseRef}:\n${wildcardFailures.join('\n')}`,
    );
  }
  const relativeBaseline = path.relative(repositoryRoot, baselineFile).split('\\').join('/');
  const baseFile = spawnSync('git', ['show', `${baseRef}:${relativeBaseline}`], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  if (baseFile.status !== 0) {
    console.log(`architecture ratchet BOOTSTRAP ${relativeBaseline} is not present at ${baseRef}`);
    return;
  }
  const failures = compareArchitectureBaselines(
    JSON.parse(fs.readFileSync(baselineFile, 'utf8')),
    JSON.parse(baseFile.stdout),
  );
  if (failures.length > 0) {
    throw new Error(`Architecture debt may only stay equal or shrink relative to ${baseRef}:\n${failures.join('\n')}`);
  }
}

try {
  assertBaselineRatchet();
  const report = analyzeArchitecture(uiRoot);
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });
  fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);
  assertArchitecture(report);
  console.log(`architecture PASS ${JSON.stringify(report.summary)} sha256=${report.reportSha256}`);
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
}
