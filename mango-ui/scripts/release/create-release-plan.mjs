#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync, renameSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { indexPublishedPackages } from './release-scope-lib.mjs';
import {
  assertCompletedReleaseBaseline,
  assertReleasePlanShape,
  assertReleasePlanSourceShape,
  buildReleasePlan,
  readPendingChangesets,
  resolveReleaseMavenSourceVersion,
  sha256,
} from './release-plan-lib.mjs';
import { classifyReleasePullRequest } from './classify-release-pr.mjs';
import { assertCliReadmeProjection, CLI_README_PATH, projectCliReadmeVersion } from './release-cli-readme-lib.mjs';
import {
  assertCliFullReadmeProjection,
  assertCliFullFrontendTemplateProjection,
  CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH,
  CLI_FULL_README_TEMPLATE_PATH,
  projectCliFullReadmeTuple,
  projectCliFullFrontendTemplateVersion,
} from './release-cli-template-lib.mjs';
import {
  assertPmoVersionedFileProjection,
  PMO_VERSION_PROJECTION_PATHS,
  projectPmoVersionedFile,
} from './release-pmo-plugin-lib.mjs';
import {
  gitChangedFiles,
  readGitFile,
  resolveBaseline,
  resolveGitSource,
  restoredPublishedBaselines,
  verifyReleasePlanSource,
} from './release-repository-lib.mjs';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = resolve(workspaceRoot, '..');
const args = process.argv.slice(2);
const includeWorkingTree = args.includes('--include-working-tree');
const checkOnly = args.includes('--check');
const skipLockfile = args.includes('--skip-lockfile');
const planPath = join(workspaceRoot, '.changeset/release-plan.json');
const legacyPath = join(workspaceRoot, '.changeset/legacy-reconciliation.json');
const legacy = existsSync(legacyPath) ? readJson(legacyPath) : null;
const storedPlan = existsSync(planPath) ? readJson(planPath) : null;
const successfulBaselinePath = join(workspaceRoot, '.changeset/release-baseline.json');
const successfulBaseline = existsSync(successfulBaselinePath) ? readJson(successfulBaselinePath) : null;
const previousPlan = storedPlan?.planDigest === successfulBaseline?.planDigest ? null : storedPlan;
const packageIndex = indexPublishedPackages(workspaceRoot);
const catalog = readJson(join(repoRoot, 'mango-catalog/catalog.lock.json'));
const releaseVersions = readJson(join(workspaceRoot, 'packages/mango-cli/release-versions.json'));
const managedVersions = releaseVersions.npm ?? {};
const baseline = resolveBaseline(repoRoot, workspaceRoot, legacy);
const planInput = resolvePlanInput();
const restored = restoredPublishedBaselines({
  repoRoot,
  packageIndex,
  legacy,
  includeWorkingTree,
});
const pendingChangesets = readPendingChangesets(workspaceRoot);
if (checkOnly && storedPlan && successfulBaseline?.planDigest === storedPlan.planDigest) {
  assertCompletedReleaseBaseline(storedPlan, successfulBaseline);
  verifyPlanProjection(storedPlan, packageIndex, managedVersions, { historicalCompleted: true });
  console.log(`Completed release plan check PASS ${storedPlan.planDigest}`);
  if (pendingChangesets.length > 0) {
    console.log(`${pendingChangesets.length} pending Changeset(s) belong to the next release plan`);
  }
  process.exit(0);
}
const releaseMetadata = resolveReleaseMetadata(previousPlan, pendingChangesets);
const mavenTargetVersion = valueArg('--maven-version') || previousPlan?.maven?.targetVersion || '';
let plan = buildPlan(catalog.catalogDigest);
assertReleasePlanShape(plan);

if (checkOnly) {
  if (!previousPlan) throw new Error('release plan does not exist');
  assertEquivalentPlan(previousPlan, plan);
  verifyPlanProjection(previousPlan, packageIndex, managedVersions);
  console.log(`Release plan check PASS ${previousPlan.planDigest}`);
  console.log(
    previousPlan.order
      .map((name) => `${name}@${previousPlan.packages.find((entry) => entry.name === name).targetVersion}`)
      .join(' -> '),
  );
  process.exit(0);
}

applyPlanProjection(plan, packageIndex);
applyPmoPluginProjection(plan);
applyBusinessPmoBaselineProjection(plan);
applyCliReadmeProjection(plan);
applyCliFullFrontendTemplateProjection(plan);
applyCliFullReadmeProjection(plan);
applyExternalManagedDependencies(plan);
writeJson(
  join(workspaceRoot, 'packages/mango-cli/release-versions.json'),
  projectedManagedVersions(plan, managedVersions),
);
writeFileSync(join(workspaceRoot, releaseMetadata.notesFile), releaseMetadata.notes);
runCatalogProjection();
const projectedCatalog = readJson(join(repoRoot, 'mango-catalog/catalog.lock.json'));
if (projectedCatalog.catalogDigest !== plan.catalogDigest) {
  plan = buildPlan(projectedCatalog.catalogDigest);
  assertReleasePlanShape(plan);
}
writeJson(planPath, plan);
if (!skipLockfile) runLockfileUpdate();
runProjectionFormatting();
console.log(`Release plan written: ${planPath}`);
console.log(`Plan digest: ${plan.planDigest}`);
console.log(
  plan.order.map((name) => `${name}@${plan.packages.find((entry) => entry.name === name).targetVersion}`).join(' -> '),
);

function applyPlanProjection(releasePlan, packages) {
  const targets = new Map(releasePlan.packages.map((entry) => [entry.name, entry.targetVersion]));
  for (const entry of releasePlan.packages) {
    const workspacePackage = packages.get(entry.name);
    const packageJson = structuredClone(workspacePackage.packageJson);
    packageJson.version = entry.targetVersion;
    for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
      for (const [dependency, value] of Object.entries(packageJson[section] ?? {})) {
        if (targets.has(dependency) && value.startsWith('workspace:') && value !== 'workspace:*') {
          packageJson[section][dependency] = `workspace:${targets.get(dependency)}`;
        }
      }
    }
    writeJson(workspacePackage.packageJsonPath, packageJson);
  }
}

function applyCliReadmeProjection(releasePlan) {
  const cli = releasePlan.packages.find((entry) => entry.name === '@mango/cli');
  if (!cli) return;
  const sourceContent = readGitFile(repoRoot, releasePlan.source.commit, CLI_README_PATH);
  const projectedContent = projectCliReadmeVersion(sourceContent, cli.sourceVersion, cli.targetVersion);
  writeFileSync(join(repoRoot, CLI_README_PATH), projectedContent);
}

function applyPmoPluginProjection(releasePlan) {
  const pmo = releasePlan.packages.find((entry) => entry.name === '@mango/pmo');
  if (!pmo) return;
  for (const path of PMO_VERSION_PROJECTION_PATHS) {
    const sourceContent = readGitFile(repoRoot, releasePlan.source.commit, path);
    const projectedContent = projectPmoVersionedFile(path, sourceContent, pmo.sourceVersion, pmo.targetVersion);
    writeFileSync(join(repoRoot, path), projectedContent);
  }
}

function applyBusinessPmoBaselineProjection(releasePlan) {
  const pmo = releasePlan.packages.find((entry) => entry.name === '@mango/pmo');
  if (!pmo) return;
  const result = spawnSync(
    process.execPath,
    [join(repoRoot, 'mango-business-starter/scripts/sync-pmo-baseline.mjs'), '--write'],
    { cwd: repoRoot, stdio: 'inherit' },
  );
  if (result.status !== 0) {
    throw new Error(`business PMO baseline projection failed with exit code ${result.status ?? 1}`);
  }
}

function applyCliFullFrontendTemplateProjection(releasePlan) {
  const cli = releasePlan.packages.find((entry) => entry.name === '@mango/cli');
  if (!cli) return;
  const sourceContent = readGitFile(repoRoot, releasePlan.source.commit, CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH);
  const projectedContent = projectCliFullFrontendTemplateVersion(sourceContent, cli.sourceVersion, cli.targetVersion);
  writeFileSync(join(repoRoot, CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH), projectedContent);
}

function applyCliFullReadmeProjection(releasePlan) {
  const cli = releasePlan.packages.find((entry) => entry.name === '@mango/cli');
  const pmo = releasePlan.packages.find((entry) => entry.name === '@mango/pmo');
  if (!cli || !pmo || !releasePlan.maven?.targetVersion) return;
  const sourceContent = readGitFile(repoRoot, releasePlan.source.commit, CLI_FULL_README_TEMPLATE_PATH);
  const projectedContent = projectCliFullReadmeTuple(sourceContent, {
    mavenVersion: releasePlan.maven.targetVersion,
    pmoVersion: pmo.targetVersion,
    cliVersion: cli.targetVersion,
  });
  writeFileSync(join(repoRoot, CLI_FULL_README_TEMPLATE_PATH), projectedContent);
}

function projectedManagedVersions(releasePlan, current) {
  const next = structuredClone(readJson(join(workspaceRoot, 'packages/mango-cli/release-versions.json')));
  next.npm ||= {};
  for (const entry of releasePlan.packages) {
    if (entry.name === '@mango/cli' || Object.hasOwn(current, entry.name)) next.npm[entry.name] = entry.targetVersion;
  }
  if (releasePlan.maven) {
    next.maven ||= {};
    next.maven.mangoBackend = releasePlan.maven.targetVersion;
  }
  return next;
}

function applyExternalManagedDependencies(releasePlan) {
  const targets = new Map(releasePlan.packages.map((entry) => [entry.name, entry]));
  for (const path of collectPackageDescriptors([
    join(repoRoot, 'mango-business-starter'),
    join(workspaceRoot, 'packages/mango-cli/templates'),
  ])) {
    const packageJson = readJsonIfValid(path);
    if (!packageJson) continue;
    let changed = false;
    for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies', 'devDependencies']) {
      for (const [dependency, value] of Object.entries(packageJson[section] ?? {})) {
        const target = targets.get(dependency);
        if (target && value === target.sourceVersion) {
          packageJson[section][dependency] = target.targetVersion;
          changed = true;
        }
      }
    }
    if (changed) writeJson(path, packageJson);
  }
}

function collectPackageDescriptors(roots) {
  const files = [];
  for (const root of roots) visit(root);
  return files.sort();

  function visit(path) {
    if (!existsSync(path)) return;
    if (statSync(path).isDirectory()) {
      for (const entry of readdirSync(path)) visit(join(path, entry));
      return;
    }
    if (path.endsWith('/package.json') || path.endsWith('/package.json.template')) files.push(path);
  }
}

function verifyPlanProjection(releasePlan, packages, currentManagedVersions, { historicalCompleted = false } = {}) {
  const notesPath = join(workspaceRoot, releasePlan.release?.notesFile ?? '');
  if (!releasePlan.release?.tag || !existsSync(notesPath)) throw new Error('release tag or release notes are missing');
  const notesHash = sha256(Buffer.from(readFileSync(notesPath, 'utf8'), 'utf8'));
  if (notesHash !== releasePlan.release.notesSha256) throw new Error('release notes do not match the machine plan');
  const targetVersions = new Map(releasePlan.packages.map((entry) => [entry.name, entry.targetVersion]));
  if (!historicalCompleted) {
    verifyPmoPluginProjection(releasePlan);
    verifyCliReadmeProjection(releasePlan);
    verifyCliFullFrontendTemplateProjection(releasePlan);
    verifyCliFullReadmeProjection(releasePlan);
  }
  const currentReleaseVersions = readJson(join(workspaceRoot, 'packages/mango-cli/release-versions.json'));
  if (releasePlan.maven && currentReleaseVersions.maven?.mangoBackend !== releasePlan.maven.targetVersion) {
    throw new Error(
      `Maven CLI release matrix ${currentReleaseVersions.maven?.mangoBackend ?? '<missing>'} != plan ${releasePlan.maven.targetVersion}`,
    );
  }
  for (const entry of releasePlan.packages) {
    const packageJson = packages.get(entry.name)?.packageJson;
    if (packageJson?.version !== entry.targetVersion) {
      throw new Error(
        `${entry.name}: package version ${packageJson?.version ?? '<missing>'} != plan ${entry.targetVersion}`,
      );
    }
    if (entry.name === '@mango/cli' || Object.hasOwn(currentManagedVersions, entry.name)) {
      if (currentManagedVersions[entry.name] !== entry.targetVersion) {
        throw new Error(
          `${entry.name}: CLI release matrix ${currentManagedVersions[entry.name]} != plan ${entry.targetVersion}`,
        );
      }
    }
    for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
      for (const [dependency, value] of Object.entries(packageJson[section] ?? {})) {
        if (targetVersions.has(dependency) && value.startsWith('workspace:') && value !== 'workspace:*') {
          const expected = `workspace:${targetVersions.get(dependency)}`;
          if (value !== expected) throw new Error(`${entry.name}: ${section}.${dependency} ${value} != ${expected}`);
        }
      }
    }
  }
  const targets = new Map(releasePlan.packages.map((entry) => [entry.name, entry]));
  for (const path of collectPackageDescriptors([
    join(repoRoot, 'mango-business-starter'),
    join(workspaceRoot, 'packages/mango-cli/templates'),
  ])) {
    const descriptor = readJsonIfValid(path);
    if (!descriptor) continue;
    for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies', 'devDependencies']) {
      for (const [dependency, value] of Object.entries(descriptor[section] ?? {})) {
        const target = targets.get(dependency);
        if (target && value === target.sourceVersion) {
          throw new Error(`${path}: ${section}.${dependency} still uses release source version ${value}`);
        }
      }
    }
  }
}

function verifyPmoPluginProjection(releasePlan) {
  const pmo = releasePlan.packages.find((entry) => entry.name === '@mango/pmo');
  if (!pmo) return;
  for (const path of PMO_VERSION_PROJECTION_PATHS) {
    assertPmoVersionedFileProjection({
      path,
      sourceContent: readGitFile(repoRoot, releasePlan.source.commit, path),
      projectedContent: readFileSync(join(repoRoot, path), 'utf8'),
      sourceVersion: pmo.sourceVersion,
      targetVersion: pmo.targetVersion,
    });
  }
}

function verifyCliReadmeProjection(releasePlan) {
  const cli = releasePlan.packages.find((entry) => entry.name === '@mango/cli');
  if (!cli) return;
  assertCliReadmeProjection({
    sourceContent: readGitFile(repoRoot, releasePlan.source.commit, CLI_README_PATH),
    projectedContent: readFileSync(join(repoRoot, CLI_README_PATH), 'utf8'),
    sourceVersion: cli.sourceVersion,
    targetVersion: cli.targetVersion,
  });
}

function verifyCliFullFrontendTemplateProjection(releasePlan) {
  const cli = releasePlan.packages.find((entry) => entry.name === '@mango/cli');
  if (!cli) return;
  assertCliFullFrontendTemplateProjection({
    sourceContent: readGitFile(repoRoot, releasePlan.source.commit, CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH),
    projectedContent: readFileSync(join(repoRoot, CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH), 'utf8'),
    sourceVersion: cli.sourceVersion,
    targetVersion: cli.targetVersion,
  });
}

function verifyCliFullReadmeProjection(releasePlan) {
  const cli = releasePlan.packages.find((entry) => entry.name === '@mango/cli');
  const pmo = releasePlan.packages.find((entry) => entry.name === '@mango/pmo');
  if (!cli || !pmo || !releasePlan.maven?.targetVersion) return;
  assertCliFullReadmeProjection({
    sourceContent: readGitFile(repoRoot, releasePlan.source.commit, CLI_FULL_README_TEMPLATE_PATH),
    projectedContent: readFileSync(join(repoRoot, CLI_FULL_README_TEMPLATE_PATH), 'utf8'),
    versions: {
      mavenVersion: releasePlan.maven.targetVersion,
      pmoVersion: pmo.targetVersion,
      cliVersion: cli.targetVersion,
    },
  });
}

function assertEquivalentPlan(expected, actual) {
  assertReleasePlanShape(expected);
  const normalize = (value) => ({ ...value, generatedAt: undefined, planDigest: undefined });
  if (JSON.stringify(normalize(expected)) !== JSON.stringify(normalize(actual))) {
    throw new Error('release plan is stale; regenerate it from the current Changesets and source impact');
  }
  if (expected.planDigest !== actual.planDigest) throw new Error('release plan digest is stale');
}

function resolvePlanInput() {
  if (!checkOnly || !previousPlan) {
    const source = resolveGitSource(repoRoot);
    const committedFiles = gitChangedFiles(repoRoot, baseline.commit, source.commit);
    if (includeWorkingTree) {
      const filesWithWorkingTree = gitChangedFiles(repoRoot, baseline.commit, source.commit, true);
      if (JSON.stringify(filesWithWorkingTree) !== JSON.stringify(committedFiles)) {
        throw new Error('release plan source must be committed before planning; working-tree source cannot be sealed');
      }
    }
    return { source, sourceFiles: committedFiles };
  }

  assertReleasePlanSourceShape(previousPlan);
  const verified = verifyReleasePlanSource({
    repoRoot,
    baselineCommit: baseline.commit,
    source: previousPlan.source,
    sourceFiles: previousPlan.sourceFiles,
  });
  if (verified.projectionFiles.length > 0) {
    const classification = classifyReleasePullRequest(verified.projectionFiles);
    if (!classification.releaseOnly) {
      throw new Error(`final HEAD contains non-release changes after the planned source: ${classification.reason}`);
    }
  }
  return { source: verified.source, sourceFiles: verified.sourceFiles };
}

function runLockfileUpdate() {
  const command = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  const result = spawnSync(command, ['install', '--lockfile-only', '--ignore-scripts'], {
    cwd: workspaceRoot,
    stdio: 'inherit',
  });
  if (result.status !== 0) throw new Error(`pnpm lockfile update failed with exit code ${result.status ?? 1}`);
}

function runProjectionFormatting() {
  const command = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  const paths = [planPath];
  if (!skipLockfile) paths.push(join(workspaceRoot, 'pnpm-lock.yaml'));
  const result = spawnSync(command, ['exec', 'prettier', '--write', ...paths], {
    cwd: workspaceRoot,
    stdio: 'inherit',
  });
  if (result.status !== 0) {
    throw new Error(
      `release projection formatting failed with exit code ${result.status ?? 1}; install the governed workspace dependencies`,
    );
  }
}

function runCatalogProjection() {
  const result = spawnSync(process.execPath, [join(workspaceRoot, 'scripts/catalog/compile-catalog.mjs'), '--write'], {
    cwd: repoRoot,
    stdio: 'inherit',
  });
  if (result.status !== 0) throw new Error(`Catalog projection failed with exit code ${result.status ?? 1}`);
}

function buildPlan(catalogDigest) {
  return buildReleasePlan({
    packageIndex,
    managedVersions,
    mavenSourceVersion: resolveReleaseMavenSourceVersion(previousPlan, releaseVersions.maven?.mangoBackend),
    mavenTargetVersion,
    source: planInput.source,
    sourceFiles: planInput.sourceFiles,
    changesets: pendingChangesets,
    legacy,
    restoredPublishedBaselines: restored,
    ignoredDirectPackages: restored.map((entry) => entry.name),
    previousPlan,
    baseline,
    release: {
      tag: releaseMetadata.tag,
      title: releaseMetadata.title,
      notesFile: releaseMetadata.notesFile,
      notesSha256: releaseMetadata.notesSha256,
    },
    catalogDigest,
    mavenInventory: catalog.maven.publishableCoordinates,
    releaseArtifacts: catalog.releaseArtifacts,
  });
}

function resolveReleaseMetadata(existingPlan, changesets) {
  const existing = existingPlan?.release;
  const tag = valueArg('--tag') || existing?.tag || '';
  if (!tag) throw new Error('release plan requires --tag; immutable creation remains deferred until publish');
  const title = valueArg('--title') || existing?.title || `Mango release ${tag}`;
  const notesFile = existing?.notesFile || '.changeset/release-notes.txt';
  const existingNotesPath = join(workspaceRoot, notesFile);
  const notes = existsSync(existingNotesPath) ? readFileSync(existingNotesPath, 'utf8') : generatedNotes(changesets);
  return { tag, title, notesFile, notesSha256: sha256(Buffer.from(notes, 'utf8')), notes };
}

function generatedNotes(changesets) {
  const summaries =
    changesets.map((entry) => `- ${entry.summary}`).join('\n') || '- Release the reconciled package tuple.';
  return `## Pull Requests\n\n<!-- Add every release-bearing PR using the required template entry format. -->\n\n## Changed\n\n${summaries}\n\n## Versions\n\n<!-- Record every changed and explicitly unchanged compatibility coordinate. -->\n\n## Published Packages\n\n<!-- Record the machine-plan topology and exact coordinates. -->\n\n## Business Impact\n\n<!-- State affected and unaffected consumers, contracts, data, configuration, and operations. -->\n\n## Upgrade Estimate\n\n- Audience:\n- Engineering Effort:\n- Execution Window:\n- Service Downtime:\n- Rollback Effort:\n- Assumptions:\n\n## Upgrade Notes\n\n<!-- Give executable consumer upgrade and adaptation steps. -->\n\n## Verification\n\n<!-- Give consumer-visible assertions and exact validation entry points. -->\n\n## Rollback\n\n<!-- Give reversible consumer rollback steps without mutating immutable release coordinates. -->\n`;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function readJsonIfValid(path) {
  try {
    return readJson(path);
  } catch (error) {
    if (path.endsWith('.template')) return null;
    throw error;
  }
}

function writeJson(path, value) {
  const temporary = `${path}.tmp`;
  writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`);
  renameSync(temporary, path);
}

function valueArg(name) {
  const inline = args.find((arg) => arg.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? (args[index + 1] ?? '') : '';
}
