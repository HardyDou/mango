import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = resolve(packageRoot, '../..');
const repoRoot = resolve(workspaceRoot, '..');
const packagesRoot = join(workspaceRoot, 'packages');
const appsRoot = join(workspaceRoot, 'apps');
const businessStarterRoot = join(repoRoot, 'mango-business-starter');
const releaseVersionsPath = join(packageRoot, 'release-versions.json');
const releaseVersions = JSON.parse(readFileSync(releaseVersionsPath, 'utf8'));
const packageIndex = indexWorkspacePackages();
const uiDependencyLocks = {
  'element-plus': '2.14.1',
  '@element-plus/icons-vue': '2.3.2',
};
const uiDependencyTemplateLocks = {
  'element-plus': '{{elementPlusVersion}}',
  '@element-plus/icons-vue': '{{iconsVueVersion}}',
};

const registryArg = process.argv.find((arg) => arg.startsWith('--registry='));
const registry = registryArg?.slice('--registry='.length);
const checkRegistry = process.argv.includes('--check-registry');
const ignoredRegistryPackages = new Set(process.argv
  .filter((arg) => arg.startsWith('--ignore-registry-package='))
  .map((arg) => arg.slice('--ignore-registry-package='.length)));
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
  if (checkRegistry && !ignoredRegistryPackages.has(packageName)) {
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

for (const [packageName, workspacePackage] of packageIndex) {
  for (const dependencyType of ['dependencies', 'peerDependencies', 'devDependencies']) {
    const dependencies = workspacePackage.packageJson[dependencyType] ?? {};
    for (const [dependencyName, declaredVersion] of Object.entries(dependencies)) {
      const dependencyPackage = packageIndex.get(dependencyName);
      if (!dependencyPackage || declaredVersion === 'workspace:*') {
        continue;
      }
      if (declaredVersion !== dependencyPackage.version) {
        mismatches.push(
          `${packageName}: ${dependencyType}.${dependencyName} ${declaredVersion} != local package ${dependencyPackage.version}`,
        );
      }
    }
  }
}

for (const packageJsonPath of collectPackageJsonFiles([
  appsRoot,
  packagesRoot,
  businessStarterRoot,
  join(packageRoot, 'templates'),
])) {
  const packageJson = parsePackageJson(packageJsonPath);
  for (const dependencyType of ['dependencies', 'peerDependencies', 'devDependencies']) {
    const dependencies = packageJson[dependencyType] ?? {};
    for (const [dependencyName, lockedVersion] of Object.entries(uiDependencyLocks)) {
      const declaredVersion = dependencies[dependencyName];
      const templateLockedVersion = uiDependencyTemplateLocks[dependencyName];
      if (
        declaredVersion
        && declaredVersion !== lockedVersion
        && declaredVersion !== templateLockedVersion
      ) {
        mismatches.push(
          `${relativePath(packageJsonPath)}: ${dependencyType}.${dependencyName} ${declaredVersion} != ${lockedVersion}`,
        );
      }
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
  if (ignoredRegistryPackages.size > 0) {
    console.log(`Skipped registry check for: ${[...ignoredRegistryPackages].join(', ')}`);
  }
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
        packageJson,
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

function parsePackageJson(packageJsonPath) {
  const content = readFileSync(packageJsonPath, 'utf8')
    .split('\n')
    .filter((line) => !line.trim().match(/^{{[a-zA-Z0-9]+}}$/))
    .join('\n');
  return JSON.parse(content);
}

function collectPackageJsonFiles(roots) {
  const packageJsonFiles = [];
  for (const root of roots) {
    collectPackageJsonFilesFromRoot(root, packageJsonFiles);
  }
  return packageJsonFiles;
}

function collectPackageJsonFilesFromRoot(root, packageJsonFiles) {
  if (!existsSync(root)) {
    return;
  }
  const stats = statSync(root);
  if (stats.isFile()) {
    if (root.endsWith('package.json') || root.endsWith('package.json.template')) {
      packageJsonFiles.push(root);
    }
    return;
  }
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist' || entry.name === 'coverage') {
      continue;
    }
    const entryPath = join(root, entry.name);
    if (entry.isDirectory()) {
      collectPackageJsonFilesFromRoot(entryPath, packageJsonFiles);
      continue;
    }
    if (entry.isFile() && (entry.name === 'package.json' || entry.name === 'package.json.template')) {
      packageJsonFiles.push(entryPath);
    }
  }
}

function relativePath(path) {
  return path.slice(repoRoot.length + 1);
}
