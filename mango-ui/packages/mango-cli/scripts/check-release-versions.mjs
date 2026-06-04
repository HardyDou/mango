import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = resolve(packageRoot, '../..');
const packagesRoot = join(workspaceRoot, 'packages');
const releaseVersionsPath = join(packageRoot, 'release-versions.json');
const releaseVersions = JSON.parse(readFileSync(releaseVersionsPath, 'utf8'));
const packageIndex = indexWorkspacePackages();

const registryArg = process.argv.find((arg) => arg.startsWith('--registry='));
const registry = registryArg?.slice('--registry='.length);
const checkRegistry = process.argv.includes('--check-registry');
const mismatches = [];

for (const [packageName, lockedVersion] of Object.entries(releaseVersions.npm ?? {})) {
  const workspacePackage = packageIndex.get(packageName);
  if (!workspacePackage) {
    mismatches.push(`${packageName}: missing local package under packages/*`);
    continue;
  }
  if (workspacePackage.version !== lockedVersion) {
    mismatches.push(`${packageName}: release lock ${lockedVersion} != local package ${workspacePackage.version}`);
  }
  if (checkRegistry) {
    if (!registry) {
      mismatches.push(`${packageName}: --check-registry requires --registry=<npm-registry-url>`);
      continue;
    }
    const publishedVersion = npmViewVersion(packageName, registry);
    if (publishedVersion !== lockedVersion) {
      mismatches.push(`${packageName}: release lock ${lockedVersion} != registry ${publishedVersion || '<not found>'}`);
    }
  }
}

if (mismatches.length > 0) {
  console.error(`release-versions.json is inconsistent:\n${mismatches.map((item) => `- ${item}`).join('\n')}`);
  process.exit(1);
}

console.log(`release-versions.json matches ${Object.keys(releaseVersions.npm ?? {}).length} local package versions.`);
if (checkRegistry) {
  console.log(`release-versions.json also matches registry ${registry}.`);
}

function indexWorkspacePackages() {
  const packages = new Map();
  for (const dir of readdirSync(packagesRoot)) {
    const packageJsonPath = join(packagesRoot, dir, 'package.json');
    if (!existsSync(packageJsonPath)) {
      continue;
    }
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    if (packageJson.name?.startsWith('@mango/')) {
      packages.set(packageJson.name, {
        version: packageJson.version,
        packageJsonPath,
      });
    }
  }
  return packages;
}

function npmViewVersion(packageName, registryUrl) {
  const result = spawnSync('npm', ['view', packageName, 'version', `--registry=${registryUrl}`], {
    stdio: 'pipe',
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    return '';
  }
  return result.stdout.trim();
}
