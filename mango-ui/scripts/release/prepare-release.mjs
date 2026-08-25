#!/usr/bin/env node
import { createHash } from 'node:crypto';
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { homedir, tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';
import { assertReleasePlanShape } from './release-plan-lib.mjs';
import {
  assertPreparedCandidate,
  buildSealedArtifactManifest,
  createPublicationJournal,
  digestRelease,
  preparedCandidateId,
} from './release-manifest-lib.mjs';
import {
  createCandidateMavenConsumerPom,
  createCandidateMavenSettings,
  inspectStagedMavenRepository,
  verifyStagedMavenRepository,
} from './release-maven-lib.mjs';
import {
  archiveFailedPrepare,
  archiveSupersededPrepare,
  isRetryablePrepareFailure,
  isSupersededLocalCandidate,
  orderExactMavenCoordinateSet,
} from './release-prepare-lib.mjs';
import { assertCleanWorktree, gitValue } from './release-repository-lib.mjs';
import { acquireReleaseLock } from './release-lock-lib.mjs';
import { verifyPmoPackageRoot } from './pmo-package-verifier-lib.mjs';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = resolve(workspaceRoot, '..');
const args = process.argv.slice(2);
const planPath = resolveArg('--plan', join(workspaceRoot, '.changeset/release-plan.json'));
const runtimeBase = resolveArg('--runtime-dir', join(repoRoot, '.runtime/mango-release'));
const consumeRegistry = valueArg('--consume-registry') || process.env.MANGO_RELEASE_NPM_CONSUME_REGISTRY || '';
const plan = readJson(planPath);
assertReleasePlanShape(plan);
const gitCommonDir = resolve(repoRoot, gitValue(repoRoot, ['rev-parse', '--git-common-dir']));
const releaseLock = acquireReleaseLock(join(gitCommonDir, 'mango-release/release.lock'), {
  releasePlanDigest: plan.planDigest,
});
process.on('exit', releaseLock);
if (plan.order.length > 0 && !consumeRegistry) {
  throw new Error('prepare requires --consume-registry or MANGO_RELEASE_NPM_CONSUME_REGISTRY for npm candidates');
}
if (consumeRegistry) assertRegistryUrl(consumeRegistry, 'npm consume registry');
assertCleanWorktree(repoRoot);
runChecked(process.execPath, [join(workspaceRoot, 'scripts/release/check-release-plan.mjs')], repoRoot);

const source = {
  commit: gitValue(repoRoot, ['rev-parse', 'HEAD^{commit}']),
  tree: gitValue(repoRoot, ['rev-parse', 'HEAD^{tree}']),
};
const releaseRoot = join(runtimeBase, plan.planDigest);
const artifactRoot = join(releaseRoot, 'artifacts');
const manifestPath = join(releaseRoot, 'manifest.json');
if (existsSync(manifestPath)) {
  const existing = readJson(manifestPath);
  if (isRetryablePrepareFailure(existing, plan, source)) {
    const archived = archiveFailedPrepare(releaseRoot);
    console.log(`Archived failed local prepare evidence: ${archived}`);
  } else if (isSupersededLocalCandidate(existing, plan, source)) {
    const archived = archiveSupersededPrepare(releaseRoot);
    console.log(`Archived superseded local candidate evidence: ${archived}`);
  } else {
    verifyPreparedManifest(existing, plan, source, artifactRoot);
    if (['READY', 'PUBLISHING', 'PARTIAL', 'AMBIGUOUS', 'REPAIR', 'COMPLETED'].includes(existing.status)) {
      console.log(`Release prepare already complete: ${manifestPath}`);
      process.exit(0);
    }
    throw new Error(`existing prepare manifest is not reusable: ${existing.status}`);
  }
}
if (existsSync(releaseRoot) && readdirSync(releaseRoot).length > 0) {
  throw new Error(`prepare directory is not empty and has no reusable manifest: ${releaseRoot}`);
}
if (existsSync(releaseRoot)) rmSync(releaseRoot, { recursive: true });
mkdirSync(artifactRoot, { recursive: true });

const startedAt = new Date().toISOString();
try {
  const releaseNotesEvidence = verifyReleaseNotes(plan);
  const buildEvidence = buildReleasePackages(plan);
  runChecked('pnpm', ['package-exports:check'], workspaceRoot);
  const artifacts = packReleasePackages(plan, artifactRoot);
  verifyPackageSpecificContracts(artifacts);
  const maven = stageMavenRelease(plan, artifactRoot);
  const sourceArchive = join(releaseRoot, `source-${source.commit}.tar`);
  runChecked('git', ['archive', '--format=tar', '--output', sourceArchive, source.commit], repoRoot);
  const sealedArtifactManifest = buildSealedArtifactManifest({
    releasePlanDigest: plan.planDigest,
    source,
    npmArtifacts: artifacts,
    mavenArtifacts: maven,
    sourceArchive: {
      path: relativeRuntimePath(sourceArchive),
      size: statSync(sourceArchive).size,
      sha256: sha256File(sourceArchive),
    },
  });
  const manifest = {
    schemaVersion: 1,
    planDigest: plan.planDigest,
    preparedCandidateId: preparedCandidateId(sealedArtifactManifest),
    sealedArtifactManifest,
    planPath: relativeRepoPath(planPath),
    status: 'PREPARED',
    remoteWrites: false,
    source: {
      ...source,
      archive: relativeRuntimePath(sourceArchive),
      archiveSha256: sha256File(sourceArchive),
      archiveSize: statSync(sourceArchive).size,
    },
    artifacts,
    packageOrder: plan.order,
    states: {
      PREPARED: state(
        'passed',
        startedAt,
        `built and sealed ${artifacts.length} exact npm tarball(s)${maven ? ` and ${maven.coordinateCount} Maven coordinate(s)` : ''}`,
      ),
      CANDIDATE_VERIFIED: state('pending', null, 'mixed candidate consumer has not run'),
      READY: state('pending', null, 'candidate consumer has not accepted the sealed candidate'),
      PUBLISHED: state('pending', null, 'immutable publication has not been authorized'),
      CONSUMER_VERIFIED: state('pending', null, 'pure consume-registry verification has not run'),
      COMPLETED: state('pending', null, 'tag and GitHub Release are intentionally deferred'),
    },
    packagePublications: Object.fromEntries(plan.order.map((name) => [name, { status: 'PENDING', attempts: [] }])),
    publicationJournal: {
      npm: createPublicationJournal(
        plan.order,
        new Map(
          artifacts.map((artifact) => [
            artifact.name,
            { preparedCandidateId: preparedCandidateId(sealedArtifactManifest), sha256: artifact.sha256 },
          ]),
        ),
      ),
      maven: createPublicationJournal(
        (maven?.coordinates ?? []).map((entry) => entry.coordinate),
        new Map(
          (maven?.coordinates ?? []).map((entry) => [
            entry.coordinate,
            { preparedCandidateId: preparedCandidateId(sealedArtifactManifest), sha256: digestRelease(entry) },
          ]),
        ),
      ),
    },
    maven,
    mavenPublications: Object.fromEntries(
      (maven?.coordinates ?? []).map((entry) => [entry.coordinate, { status: 'PENDING', attempts: [] }]),
    ),
    evidence: { releaseNotes: releaseNotesEvidence, build: buildEvidence, candidate: null, consumer: null },
    createdAt: startedAt,
    updatedAt: new Date().toISOString(),
  };
  writeJson(manifestPath, manifest);

  const candidateStartedAt = new Date().toISOString();
  const npmCandidate =
    plan.order.length > 0
      ? runCaptured(
          'pnpm',
          [
            'package-consumer:typecheck',
            '--',
            '--release-candidate-matrix',
            '--reuse-build',
            `--candidate-dir=${artifactRoot}`,
            `--registry=${consumeRegistry}`,
          ],
          workspaceRoot,
          60 * 60 * 1000,
        )
      : null;
  const mavenCandidate = maven ? verifyMavenCandidate(maven, artifactRoot) : [];
  manifest.evidence.candidate = {
    npm: npmCandidate ? evidence(npmCandidate, candidateStartedAt, workspaceRoot) : null,
    maven: mavenCandidate,
  };
  manifest.updatedAt = new Date().toISOString();
  if ((npmCandidate && npmCandidate.status !== 0) || mavenCandidate.some((entry) => entry.exitCode !== 0)) {
    manifest.status = 'FAILED';
    manifest.states.CANDIDATE_VERIFIED = state('failed', candidateStartedAt, 'mixed candidate consumer failed');
    writeJson(manifestPath, manifest);
    throw new Error(`mixed candidate consumer failed; inspect ${manifestPath}`);
  }
  manifest.status = 'READY';
  manifest.states.CANDIDATE_VERIFIED = state(
    'passed',
    candidateStartedAt,
    'mixed candidate consumer installed the sealed tarballs',
  );
  manifest.states.READY = state('passed', candidateStartedAt, 'prepared candidate identity accepted');
  writeJson(manifestPath, manifest);
  console.log(`Release candidate verified: ${manifestPath}`);
  console.log(`Source tree: ${source.tree}`);
  for (const artifact of artifacts) console.log(`${artifact.name}@${artifact.version} ${artifact.sha256}`);
} catch (error) {
  if (!existsSync(manifestPath)) {
    writeJson(manifestPath, {
      schemaVersion: 1,
      phase: 'PREPARE',
      status: 'FAILED',
      remoteWrites: false,
      planDigest: plan.planDigest,
      planPath: relativeRepoPath(planPath),
      source,
      error: String(error?.message || error).slice(0, 4000),
      createdAt: startedAt,
      updatedAt: new Date().toISOString(),
    });
  }
  throw error;
}

function buildReleasePackages(releasePlan) {
  const evidenceItems = [];
  const targets = new Set(releasePlan.order);
  const buildNames = [];
  if (targets.has('@mango/pmo')) buildNames.push('@mango/pmo');
  if (targets.has('@mango/admin')) {
    buildNames.push('@mango/admin');
    targets.delete('@mango/admin-shell');
    targets.delete('@mango/admin');
  }
  for (const entry of releasePlan.packages) {
    if (!targets.has(entry.name) || buildNames.includes(entry.name)) continue;
    const packageJson = readJson(join(workspaceRoot, entry.directory, 'package.json'));
    if (packageJson.scripts?.build) buildNames.push(entry.name);
  }
  for (const name of buildNames) {
    const started = new Date().toISOString();
    const result = runCaptured('pnpm', ['--filter', name, 'run', 'build'], workspaceRoot, 60 * 60 * 1000);
    evidenceItems.push(evidence(result, started, workspaceRoot));
    if (result.status !== 0) throw new Error(`build failed for ${name}`);
  }
  return evidenceItems;
}

function verifyReleaseNotes(releasePlan) {
  const results = [];
  for (const entry of releasePlan.packages) {
    const commandArgs = [
      join(workspaceRoot, 'scripts/check-release-notes.mjs'),
      `--package=${entry.name}`,
      `--version=${entry.targetVersion}`,
      `--tag=${releasePlan.release.tag}`,
      `--notes-file=${releasePlan.release.notesFile}`,
    ];
    const started = new Date().toISOString();
    const result = runCaptured(process.execPath, commandArgs, repoRoot);
    results.push(evidence(result, started, repoRoot));
    if (result.status !== 0) throw new Error(`release notes check failed for ${entry.name}@${entry.targetVersion}`);
  }
  return results;
}

function packReleasePackages(releasePlan, destination) {
  for (const entry of releasePlan.packages) {
    runChecked(
      'pnpm',
      ['--config.ignore-scripts=true', 'pack', '--pack-destination', destination],
      join(workspaceRoot, entry.directory),
    );
  }
  const artifacts = readdirSync(destination)
    .filter((file) => file.endsWith('.tgz'))
    .map((file) => inspectTarball(join(destination, file)))
    .sort((left, right) => releasePlan.order.indexOf(left.name) - releasePlan.order.indexOf(right.name));
  if (artifacts.length !== releasePlan.packages.length) {
    throw new Error(`packed ${artifacts.length} tarballs for ${releasePlan.packages.length} planned packages`);
  }
  for (const entry of releasePlan.packages) {
    const artifact = artifacts.find((item) => item.name === entry.name);
    if (!artifact || artifact.version !== entry.targetVersion) {
      throw new Error(
        `${entry.name}: sealed tarball version ${artifact?.version ?? '<missing>'} != ${entry.targetVersion}`,
      );
    }
  }
  return artifacts;
}

function inspectTarball(path) {
  const packageResult = runCaptured('tar', ['-xOf', path, 'package/package.json'], repoRoot);
  if (packageResult.status !== 0) throw new Error(`cannot read package.json from ${path}`);
  const packageJson = JSON.parse(packageResult.stdout);
  const filesResult = runCaptured('tar', ['-tzf', path], repoRoot);
  if (filesResult.status !== 0) throw new Error(`cannot list ${path}`);
  const fileList = filesResult.stdout.split(/\r?\n/u).filter(Boolean).sort();
  const bytes = readFileSync(path);
  return {
    name: packageJson.name,
    version: packageJson.version,
    file: relativeRuntimePath(path),
    sha256: createHash('sha256').update(bytes).digest('hex'),
    sri: `sha512-${createHash('sha512').update(bytes).digest('base64')}`,
    size: statSync(path).size,
    fileCount: fileList.length,
    fileListDigest: createHash('sha256').update(fileList.join('\n')).digest('hex'),
    publishedRanges: collectPublishedRanges(packageJson),
  };
}

function collectPublishedRanges(packageJson) {
  const result = [];
  for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
    for (const [name, range] of Object.entries(packageJson[section] ?? {})) {
      if (name.startsWith('@mango/')) result.push({ name, range, section });
    }
  }
  return result.sort((left, right) => `${left.name}\0${left.section}`.localeCompare(`${right.name}\0${right.section}`));
}

function verifyPackageSpecificContracts(artifacts) {
  const pmo = artifacts.find((artifact) => artifact.name === '@mango/pmo');
  if (!pmo) return;
  const temporary = mkdtempSync(join(tmpdir(), 'mango-pmo-candidate-'));
  try {
    runChecked('tar', ['-xzf', resolve(releaseRoot, pmo.file), '-C', temporary], repoRoot);
    verifyPmoPackageRoot(join(temporary, 'package'));
  } finally {
    rmSync(temporary, { recursive: true, force: true });
  }
}

function verifyPreparedManifest(manifest, releasePlan, currentSource, artifactsDirectory) {
  if (manifest.schemaVersion !== 1 || manifest.planDigest !== releasePlan.planDigest) {
    throw new Error('prepare manifest does not match the current release plan');
  }
  if (manifest.source?.tree !== currentSource.tree) throw new Error('prepare manifest source tree differs from HEAD');
  for (const artifact of manifest.artifacts ?? []) {
    const path = join(artifactsDirectory, artifact.file.split('/').at(-1));
    if (!existsSync(path) || sha256File(path) !== artifact.sha256) {
      throw new Error(`sealed artifact hash mismatch: ${artifact.name}@${artifact.version}`);
    }
  }
  assertPreparedCandidate({
    manifest,
    planDigest: releasePlan.planDigest,
    source: currentSource,
    artifacts: manifest.artifacts ?? [],
    mavenArtifacts: manifest.maven,
    sourceArchive: {
      path: manifest.source.archive,
      size: manifest.source.archiveSize,
      sha256: manifest.source.archiveSha256,
    },
  });
  if (manifest.maven) {
    verifyStagedMavenRepository(join(artifactsDirectory, 'maven-repository'), manifest.maven);
  }
}

function stageMavenRelease(releasePlan, destination) {
  if (!releasePlan.maven) return null;
  const repository = join(destination, 'maven-repository');
  mkdirSync(repository, { recursive: true });
  runChecked(
    join(repoRoot, 'scripts/publish-maven-batch.sh'),
    [
      '--all-non-app',
      '--release-version',
      releasePlan.maven.targetVersion,
      '--repository-id',
      'mango-release-stage',
      '--repository-url',
      pathToFileURL(repository).href,
      '--skip-verify',
    ],
    repoRoot,
  );
  const staged = inspectStagedMavenRepository(repository, releasePlan.maven.targetVersion);
  const coordinates = orderExactMavenCoordinateSet(releasePlan.maven.order, staged.coordinates);
  return {
    ...staged,
    coordinates,
    repository: 'artifacts/maven-repository',
    scope: releasePlan.maven.scope,
    docsBundle: releasePlan.maven.docsBundle,
  };
}

function verifyMavenCandidate(descriptor, destination) {
  const repositoryId = 'mango-release-stage';
  const repository = join(destination, 'maven-repository');
  const localRepository = join(releaseRoot, 'maven-candidate-local-repository');
  const outputDirectory = join(releaseRoot, 'maven-candidate-resolved-artifacts');
  const consumerPom = join(releaseRoot, 'maven-candidate-consumer.pom.xml');
  const candidateSettings = join(releaseRoot, 'maven-candidate-settings.xml');
  const userSettings = resolve(process.env.MANGO_RELEASE_MAVEN_SETTINGS || join(homedir(), '.m2/settings.xml'));
  mkdirSync(localRepository, { recursive: true });
  mkdirSync(outputDirectory, { recursive: true });
  writeFileSync(
    consumerPom,
    createCandidateMavenConsumerPom(descriptor.coordinates, repositoryId, pathToFileURL(repository).href),
    { mode: 0o600 },
  );
  writeFileSync(
    candidateSettings,
    createCandidateMavenSettings(existsSync(userSettings) ? readFileSync(userSettings, 'utf8') : '', repositoryId),
    { mode: 0o600 },
  );
  try {
    const startedAt = new Date().toISOString();
    const result = runCaptured(
      'mvn',
      [
        '-q',
        '-U',
        ...(existsSync(userSettings) ? ['-gs', userSettings] : []),
        '-s',
        candidateSettings,
        '-f',
        consumerPom,
        'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies',
        `-Dmaven.repo.local=${localRepository}`,
        '-DexcludeTransitive=true',
        '-Dmdep.useRepositoryLayout=true',
        `-DoutputDirectory=${outputDirectory}`,
      ],
      repoRoot,
      20 * 60 * 1000,
    );
    return [evidence(result, startedAt, repoRoot)];
  } finally {
    rmSync(candidateSettings, { force: true });
    rmSync(consumerPom, { force: true });
    rmSync(localRepository, { recursive: true, force: true });
    rmSync(outputDirectory, { recursive: true, force: true });
  }
}

function state(status, started, reason) {
  return { status, reason, updatedAt: new Date().toISOString(), startedAt: started };
}

function evidence(result, startedAt, cwd) {
  return {
    command: result.command,
    cwd,
    startedAt,
    completedAt: new Date().toISOString(),
    exitCode: result.status,
    output: `${result.stdout}${result.stderr}`.trim().slice(-20000) || '[no output]',
  };
}

function runChecked(command, commandArgs, cwd) {
  const result = spawnSync(command, commandArgs, { cwd, stdio: 'inherit', encoding: 'utf8' });
  if (result.status !== 0) throw new Error(`${command} ${commandArgs.join(' ')} failed with ${result.status ?? 1}`);
}

function runCaptured(command, commandArgs, cwd, timeout = 30 * 60 * 1000) {
  const result = spawnSync(command, commandArgs, { cwd, encoding: 'utf8', timeout });
  return {
    command: [command, ...commandArgs].join(' '),
    status: result.status ?? 1,
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? result.error?.message ?? '',
  };
}

function sha256File(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function writeJson(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  const temporary = `${path}.tmp`;
  writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
  renameSync(temporary, path);
}

function valueArg(name) {
  const inline = args.find((arg) => arg.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? (args[index + 1] ?? '') : '';
}

function resolveArg(name, fallback) {
  return resolve(valueArg(name) || fallback);
}

function assertRegistryUrl(value, label) {
  const url = new URL(value);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
    throw new Error(`${label} must be an HTTP(S) URL without credentials`);
  }
}

function relativeRepoPath(path) {
  return path.startsWith(`${repoRoot}/`) ? path.slice(repoRoot.length + 1) : path;
}

function relativeRuntimePath(path) {
  return path.startsWith(`${releaseRoot}/`) ? path.slice(releaseRoot.length + 1) : path;
}
