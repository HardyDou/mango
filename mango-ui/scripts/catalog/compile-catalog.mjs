#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { analyzeArchitecture, assertArchitecture } from '../quality/architecture-graph-lib.mjs';
import {
  assertCatalogProjection,
  assertFileCopyProjection,
  canonicalJsonBytes,
  compileCatalog,
} from './catalog-lib.mjs';
import { exportMavenInventory, indexMavenModulePaths } from './maven-effective-model-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const catalogFile = path.join(repositoryRoot, 'mango-catalog/catalog.lock.json');
const adminManifestFiles = [
  path.join(uiRoot, 'packages/admin/admin-modules.json'),
  path.join(uiRoot, 'packages/mango-cli/admin-modules.json'),
];
const cliModuleProjectionFile = path.join(uiRoot, 'packages/mango-cli/module-projections.json');
const runtimeRoot = path.join(repositoryRoot, '.runtime/catalog');
const effectivePomArgument = process.argv.find((argument) => argument.startsWith('--effective-pom='));
const write = process.argv.includes('--write');
const check = process.argv.includes('--check') || !write;

if (write && process.argv.includes('--check')) throw new Error('--write and --check are mutually exclusive');
for (const argument of process.argv.slice(2)) {
  if (!['--', '--write', '--check'].includes(argument) && !argument.startsWith('--effective-pom=')) {
    throw new Error(`unknown catalog compiler argument: ${argument}`);
  }
}

try {
  const effectivePomFile = effectivePomArgument
    ? path.resolve(effectivePomArgument.slice('--effective-pom='.length))
    : generateEffectivePom();
  const architectureReport = analyzeArchitecture(uiRoot);
  assertArchitecture(architectureReport);
  const modulePaths = indexMavenModulePaths(path.join(repositoryRoot, 'mango'));
  const mavenInventory = exportMavenInventory(fs.readFileSync(effectivePomFile, 'utf8'), modulePaths);
  const catalog = compileCatalog({ repositoryRoot, architectureReport, mavenInventory });
  const output = canonicalJsonBytes(catalog);

  if (write) {
    fs.mkdirSync(path.dirname(catalogFile), { recursive: true });
    const temporaryFile = `${catalogFile}.tmp-${process.pid}`;
    fs.writeFileSync(temporaryFile, output);
    fs.renameSync(temporaryFile, catalogFile);
    for (const projectionFile of adminManifestFiles)
      writeProjection(projectionFile, canonicalJsonBytes(catalog.projections.adminModules));
    writeProjection(cliModuleProjectionFile, canonicalJsonBytes(catalog.projections.cliModules));
    writeResourceCopies(catalog);
    console.log(catalogSummary('WRITE', catalog));
  } else if (check) {
    assertCatalogProjection(catalogFile, output);
    for (const projectionFile of adminManifestFiles) {
      assertCatalogProjection(projectionFile, canonicalJsonBytes(catalog.projections.adminModules));
    }
    assertCatalogProjection(cliModuleProjectionFile, canonicalJsonBytes(catalog.projections.cliModules));
    assertResourceCopies(catalog);
    console.log(catalogSummary('PASS', catalog));
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
}

function writeProjection(file, bytes) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const temporaryFile = `${file}.tmp-${process.pid}`;
  fs.writeFileSync(temporaryFile, bytes);
  fs.renameSync(temporaryFile, file);
}

function writeResourceCopies(catalog) {
  for (const { source, target } of resolveResourceCopies(catalog)) {
    fs.mkdirSync(path.dirname(target), { recursive: true });
    const temporaryFile = `${target}.tmp-${process.pid}`;
    fs.copyFileSync(source, temporaryFile);
    fs.renameSync(temporaryFile, target);
  }
}

function assertResourceCopies(catalog) {
  for (const { source, target } of resolveResourceCopies(catalog)) {
    assertFileCopyProjection(source, target);
  }
}

function resolveResourceCopies(catalog) {
  const packagePaths = new Map(catalog.packages.map((record) => [record.name, record.path]));
  return catalog.modules.flatMap((module) =>
    (module.resourceCopies || []).map((resource) => ({
      source: path.join(repositoryRoot, resource.source),
      target: path.join(repositoryRoot, packagePaths.get(resource.targetOwner), resource.targetPath),
    })),
  );
}

function generateEffectivePom() {
  fs.mkdirSync(runtimeRoot, { recursive: true });
  const outputFile = path.join(runtimeRoot, 'reactor-effective-pom.xml');
  const result = spawnSync(
    'mvn',
    ['-q', '-f', path.join(repositoryRoot, 'mango/pom.xml'), 'help:effective-pom', `-Doutput=${outputFile}`],
    { cwd: repositoryRoot, encoding: 'utf8' },
  );
  if (result.status !== 0) {
    throw new Error(
      `Maven Effective Model export failed:\n${[result.stdout, result.stderr].filter(Boolean).join('\n')}`,
    );
  }
  if (!fs.existsSync(outputFile)) throw new Error(`Maven Effective Model output is missing: ${outputFile}`);
  return mergeStandaloneParentPom(outputFile);
}

// `mango-parent` 通过 `relativePath` 供全仓父链，本身不在 reactor 内，因此 `help:effective-pom`
// 不会包含它；但每个发布坐标的 POM 都以它为 parent，消费方必须能解析到，发布集合里必须带上。
function mergeStandaloneParentPom(reactorEffectivePomFile) {
  const parentPomFile = path.join(repositoryRoot, 'mango/mango-parent/pom.xml');
  if (!fs.existsSync(parentPomFile)) return reactorEffectivePomFile;
  const standaloneFile = path.join(runtimeRoot, 'mango-parent-effective-pom.xml');
  const result = spawnSync(
    'mvn',
    ['-q', '-f', parentPomFile, 'help:effective-pom', `-Doutput=${standaloneFile}`],
    { cwd: repositoryRoot, encoding: 'utf8' },
  );
  if (result.status !== 0) {
    throw new Error(
      `Maven Effective Model export failed for mango-parent:\n${[result.stdout, result.stderr].filter(Boolean).join('\n')}`,
    );
  }
  const reactorXml = fs.readFileSync(reactorEffectivePomFile, 'utf8');
  const standaloneXml = fs.readFileSync(standaloneFile, 'utf8');
  const projectMatch = /<project\b[\s\S]*?<\/project>/.exec(standaloneXml);
  if (!projectMatch) throw new Error(`Maven Effective Model for mango-parent has no project element`);
  if (!/<projects[\s>]/u.test(reactorXml)) {
    throw new Error(`Maven Effective Model export is not a multi-project document`);
  }
  const merged = reactorXml.replace('</projects>', `${projectMatch[0]}\n</projects>`);
  const mergedFile = path.join(runtimeRoot, 'reactor-effective-pom-with-parent.xml');
  fs.writeFileSync(mergedFile, merged);
  return mergedFile;
}

function catalogSummary(status, catalog) {
  return [
    `catalog ${status}`,
    `packages=${catalog.packages.length}`,
    `modules=${catalog.modules.length}`,
    `maven=${catalog.maven.publishableCoordinateCount}`,
    `releaseArtifacts=${catalog.releaseArtifacts.length}`,
    `sha256=${catalog.catalogDigest}`,
  ].join(' ');
}
