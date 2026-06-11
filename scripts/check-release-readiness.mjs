#!/usr/bin/env node
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const currentFile = fileURLToPath(import.meta.url);
const repoRoot = resolve(dirname(currentFile), '..');
const uiRoot = join(repoRoot, 'mango-ui');
const uiPackagesRoot = join(uiRoot, 'packages');
const cliRoot = join(uiPackagesRoot, 'mango-cli');
const cliSourcePath = join(cliRoot, 'src/index.mjs');
const releaseVersionsPath = join(cliRoot, 'release-versions.json');
const fullFrontendTemplatePath = join(cliRoot, 'templates/full/frontend/package.json.template');
const adminPackagesManifestPath = join(uiPackagesRoot, 'admin/admin-packages.json');
const mavenRoot = join(repoRoot, 'mango');
const mavenPomPath = join(mavenRoot, 'pom.xml');
const checkRegistry = process.argv.includes('--check-registry');
const npmGroupRegistry = readArgValue('--npm-registry')
  || process.env.npm_config_registry
  || process.env.NPM_CONFIG_REGISTRY
  || 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/';

const errors = [];
const warnings = [];
const notes = [];

checkRequiredFiles();

const releaseVersions = readJson(releaseVersionsPath);
const cliSource = readText(cliSourcePath);
const frontendTemplate = readText(fullFrontendTemplatePath);
const npmPackages = indexNpmPackages();
const cliFrontendPackages = collectCliFrontendPackages(cliSource);
const cliBackendArtifacts = collectCliBackendArtifacts(cliSource);
const mavenArtifacts = indexMavenArtifacts();

checkReleaseVersionLocks();
checkNpmPackages();
checkCliFrontendPackages();
checkCliTemplateDependencies();
checkAdminStyleManifest();
checkCliBackendArtifacts();
checkPublishEntrypoints();

printReport();

if (errors.length > 0) {
  process.exit(1);
}

function checkRequiredFiles() {
  for (const path of [
    releaseVersionsPath,
    cliSourcePath,
    fullFrontendTemplatePath,
    adminPackagesManifestPath,
    mavenPomPath,
  ]) {
    if (!existsSync(path)) {
      errors.push(`missing required release file: ${relativePath(path)}`);
    }
  }
}

function checkReleaseVersionLocks() {
  const args = [join(cliRoot, 'scripts/check-release-versions.mjs')];
  if (checkRegistry) {
    args.push('--check-registry', `--registry=${npmGroupRegistry}`);
  }
  const result = spawnSync(process.execPath, args, {
    cwd: repoRoot,
    encoding: 'utf8',
    stdio: 'pipe',
  });
  if (result.status !== 0) {
    errors.push(`CLI release version lock check failed:\n${indent((result.stdout || '') + (result.stderr || ''))}`);
    return;
  }
  notes.push(trimLines(result.stdout));
}

function checkNpmPackages() {
  const releaseLocked = new Set(Object.keys(releaseVersions.npm ?? {}));
  for (const packageName of releaseLocked) {
    const npmPackage = npmPackages.get(packageName);
    if (!npmPackage) {
      errors.push(`${packageName}: release-versions.json points to a package missing from mango-ui/packages/*`);
      continue;
    }
    if (!npmPackage.packageJson.publishConfig?.registry) {
      errors.push(`${packageName}: missing package.json publishConfig.registry`);
    }
  }

  for (const [packageName, npmPackage] of npmPackages) {
    if (!npmPackage.packageJson.publishConfig?.registry) {
      continue;
    }
    if (packageName === '@mango/cli' || packageName === '@mango/api-schema') {
      continue;
    }
    if (!releaseLocked.has(packageName)) {
      warnings.push(`${packageName}: publishable package is not locked in mango-cli release-versions.json`);
    }
  }

  for (const [packageName, npmPackage] of npmPackages) {
    const exportsValue = npmPackage.packageJson.exports ?? {};
    const styleExport = exportsValue['./style.css'];
    if (styleExport) {
      const stylePath = typeof styleExport === 'string' ? styleExport : styleExport.import;
      const styleFile = stylePath ? join(npmPackage.dirPath, stylePath) : '';
      const files = npmPackage.packageJson.files ?? [];
      const generatedByBuild = stylePath?.startsWith('./dist/') && npmPackage.packageJson.scripts?.build && files.includes('dist');
      const packagedSourceStyle = stylePath === './style.css' && files.includes('style.css');
      if (!stylePath) {
        errors.push(`${packageName}: exports ./style.css must point to a concrete style file`);
      } else if (!existsSync(styleFile) && !generatedByBuild) {
        errors.push(`${packageName}: exports ./style.css -> ${stylePath}, but the file is missing and build/files do not publish dist`);
      }
      if (!generatedByBuild && !packagedSourceStyle) {
        errors.push(`${packageName}: exports ./style.css -> ${stylePath}, but package.json files do not publish that style path`);
      }
    }
  }
}

function checkCliFrontendPackages() {
  const releaseLocked = new Set(Object.keys(releaseVersions.npm ?? {}));
  for (const packageName of cliFrontendPackages) {
    if (!releaseLocked.has(packageName)) {
      errors.push(`${packageName}: used by mango-cli frontend generation but missing from release-versions.json`);
    }
    if (!npmPackages.has(packageName)) {
      errors.push(`${packageName}: used by mango-cli frontend generation but missing from mango-ui/packages/*`);
    }
  }
}

function checkCliTemplateDependencies() {
  const requiredTemplatePackages = [
    '@mango/admin',
    '@mango/admin-pages',
    '@mango/admin-shell',
    '@mango/app-runtime',
    '@mango/auth',
    '@mango/common',
    '@mango/rbac',
    '@mango/system',
  ];
  for (const packageName of requiredTemplatePackages) {
    if (!frontendTemplate.includes(`"${packageName}"`)) {
      errors.push(`${packageName}: missing from full frontend package.json.template dependencies`);
    }
  }

  for (const packageName of cliFrontendPackages) {
    const shortName = packageName.slice('@mango/'.length);
    const hasCliDeclaration = cliSource.includes(`frontendPackage: '${packageName}'`)
      || cliSource.includes(`name: '${packageName}'`);
    const hasCoreDependency = frontendTemplate.includes(`"${packageName}"`);
    if (!hasCliDeclaration && !hasCoreDependency && !['admin-pages'].includes(shortName)) {
      warnings.push(`${packageName}: release locked but not clearly wired to full template or OPTIONAL_MODULES`);
    }
  }
}

function checkAdminStyleManifest() {
  const manifest = readJson(adminPackagesManifestPath);
  const packages = manifest.packages ?? [];
  const seen = new Set();
  for (const item of packages) {
    if (!item.name || !item.style) {
      errors.push(`admin-packages.json: each package entry must include name and style`);
      continue;
    }
    if (seen.has(item.name)) {
      errors.push(`admin-packages.json: duplicate package ${item.name}`);
    }
    seen.add(item.name);
    const npmPackage = npmPackages.get(item.name);
    if (!npmPackage) {
      errors.push(`admin-packages.json: ${item.name} does not exist under mango-ui/packages/*`);
      continue;
    }
    if (!Object.prototype.hasOwnProperty.call(npmPackage.packageJson.exports ?? {}, './style.css')) {
      errors.push(`admin-packages.json: ${item.name} does not export ./style.css`);
    }
    if (!Object.prototype.hasOwnProperty.call(releaseVersions.npm ?? {}, item.name)) {
      errors.push(`admin-packages.json: ${item.name} is not locked in release-versions.json`);
    }
  }
}

function checkCliBackendArtifacts() {
  for (const artifactId of cliBackendArtifacts) {
    if (!mavenArtifacts.has(artifactId)) {
      errors.push(`${artifactId}: used by mango-cli backend generation but missing from Maven reactor`);
    }
  }
}

function checkPublishEntrypoints() {
  if (!existsSync(join(uiRoot, 'scripts/publish-package.mjs'))) {
    errors.push('missing mango-ui/scripts/publish-package.mjs');
  }
  if (!existsSync(join(repoRoot, 'scripts/publish-maven-module.sh'))) {
    errors.push('missing scripts/publish-maven-module.sh');
  }
}

function printReport() {
  console.log('Mango release readiness check');
  console.log(`- npm release locks: ${Object.keys(releaseVersions.npm ?? {}).length}`);
  console.log(`- CLI frontend packages: ${cliFrontendPackages.size}`);
  console.log(`- CLI backend Maven artifacts: ${cliBackendArtifacts.size}`);
  console.log(`- Maven reactor artifacts: ${mavenArtifacts.size}`);
  if (checkRegistry) {
    console.log(`- registry check: ${npmGroupRegistry}`);
  }

  if (notes.length > 0) {
    console.log('\nChecks:');
    for (const note of notes) {
      if (note) {
        console.log(indent(note, '  '));
      }
    }
  }

  if (warnings.length > 0) {
    console.log('\nWarnings:');
    for (const warning of warnings) {
      console.log(`- ${warning}`);
    }
  }

  console.log('\nRelease commands:');
  console.log('- npm package: cd mango-ui && pnpm publish:pkg <package|short-name>');
  console.log('- Maven module: scripts/publish-maven-module.sh <artifactId|module-path> --also-make');
  console.log('- CLI registry lock: cd mango-ui && pnpm --filter @mango/cli run check:release-versions -- --check-registry');

  if (errors.length > 0) {
    console.error('\nErrors:');
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    return;
  }
  console.log('\nRelease readiness checks passed.');
}

function collectCliFrontendPackages(source) {
  const packages = new Set();
  const packagePattern = /(?:name|frontendPackage):\s*'(@mango\/[^']+)'/g;
  let match;
  while ((match = packagePattern.exec(source)) !== null) {
    packages.add(match[1]);
  }
  for (const packageName of Object.keys(releaseVersions.npm ?? {})) {
    if (source.includes(packageName) || frontendTemplate.includes(packageName)) {
      packages.add(packageName);
    }
  }
  return packages;
}

function collectCliBackendArtifacts(source) {
  const artifacts = new Set();
  const artifactPattern = /artifactId:\s*'([^']+)'/g;
  let match;
  while ((match = artifactPattern.exec(source)) !== null) {
    const artifactId = match[1];
    if (artifactId.startsWith('mango-')) {
      artifacts.add(artifactId);
    }
  }
  return artifacts;
}

function indexNpmPackages() {
  const packages = new Map();
  if (!existsSync(uiPackagesRoot)) {
    return packages;
  }
  for (const dir of readdirSync(uiPackagesRoot)) {
    const packageJsonPath = join(uiPackagesRoot, dir, 'package.json');
    if (!existsSync(packageJsonPath)) {
      continue;
    }
    const packageJson = readJson(packageJsonPath);
    if (!packageJson.name?.startsWith('@mango/')) {
      continue;
    }
    packages.set(packageJson.name, {
      dir,
      dirPath: join(uiPackagesRoot, dir),
      packageJson,
    });
  }
  return packages;
}

function indexMavenArtifacts() {
  const artifacts = new Set();
  collectPomArtifacts(mavenRoot, artifacts);
  return artifacts;
}

function collectPomArtifacts(root, artifacts) {
  if (!existsSync(root) || !statSync(root).isDirectory()) {
    return;
  }
  const pomPath = join(root, 'pom.xml');
  if (existsSync(pomPath)) {
    const pom = readText(pomPath);
    const artifactId = firstXmlValue(pom, 'artifactId');
    if (artifactId) {
      artifacts.add(artifactId);
    }
  }
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (!entry.isDirectory() || entry.name === 'target' || entry.name.startsWith('.')) {
      continue;
    }
    collectPomArtifacts(join(root, entry.name), artifacts);
  }
}

function firstXmlValue(xml, tagName) {
  const withoutParent = xml.replace(/<parent>[\s\S]*?<\/parent>/, '');
  const match = withoutParent.match(new RegExp(`<${tagName}>\\s*([^<]+?)\\s*</${tagName}>`));
  return match?.[1]?.trim() ?? '';
}

function readJson(path) {
  if (!existsSync(path)) {
    return {};
  }
  return JSON.parse(readText(path));
}

function readText(path) {
  if (!existsSync(path)) {
    return '';
  }
  return readFileSync(path, 'utf8');
}

function readArgValue(name) {
  const prefix = `${name}=`;
  const value = process.argv.find((arg) => arg.startsWith(prefix));
  return value?.slice(prefix.length) ?? '';
}

function trimLines(text) {
  return text
    .split('\n')
    .map((line) => line.trimEnd())
    .filter(Boolean)
    .join('\n');
}

function indent(text, prefix = '  ') {
  return text
    .split('\n')
    .map((line) => `${prefix}${line}`)
    .join('\n');
}

function relativePath(path) {
  return relative(repoRoot, path);
}
