#!/usr/bin/env node
import { createHash } from 'node:crypto';
import {
  existsSync,
  mkdtempSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { assertReleasePlanShape, sha256 } from './release-plan-lib.mjs';
import { decideRegistryAction, markRemoteWriteIntent, recoverRemoteWriteAudit } from './release-publication-lib.mjs';
import { decideMavenCoordinateAction, verifyStagedMavenRepository } from './release-maven-lib.mjs';
import { assertCleanWorktree, gitValue } from './release-repository-lib.mjs';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = resolve(workspaceRoot, '..');
const args = process.argv.slice(2);
const action = args[0] || 'status';
const planPath = resolveArg('--plan', join(workspaceRoot, '.changeset/release-plan.json'));
const plan = readJson(planPath);
assertReleasePlanShape(plan);
const runtimeBase = resolveArg('--runtime-dir', join(repoRoot, '.runtime/mango-release'));
const releaseRoot = join(runtimeBase, plan.planDigest);
const manifestPath = join(releaseRoot, 'manifest.json');
if (!existsSync(manifestPath)) throw new Error(`prepare manifest does not exist: ${manifestPath}`);
const manifest = readJson(manifestPath);

if (action === 'status') {
  printStatus(manifest);
  process.exit(0);
}
if (!['publish', 'repair'].includes(action)) throw new Error(`unsupported prepared release action: ${action}`);
if (!args.includes('--authorize') && process.env.MANGO_RELEASE_AUTHORIZED !== '1') {
  throw new Error('publish/repair requires current-turn --authorize or MANGO_RELEASE_AUTHORIZED=1');
}
const publishRegistry = valueArg('--publish-registry') || process.env.MANGO_RELEASE_NPM_PUBLISH_REGISTRY || '';
const consumeRegistry = valueArg('--consume-registry') || process.env.MANGO_RELEASE_NPM_CONSUME_REGISTRY || '';
if (plan.order.length > 0) {
  assertRegistryUrl(publishRegistry, 'npm publish registry');
  assertRegistryUrl(consumeRegistry, 'npm consume registry');
}
const mavenPublishRegistry =
  valueArg('--maven-publish-registry') || process.env.MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY || '';
const mavenConsumeRegistry =
  valueArg('--maven-consume-registry') || process.env.MANGO_RELEASE_MAVEN_CONSUME_REGISTRY || '';
const mavenPublishServerId =
  valueArg('--maven-publish-server-id') || process.env.MANGO_RELEASE_MAVEN_PUBLISH_SERVER_ID || '';
const mavenConsumeServerId =
  valueArg('--maven-consume-server-id') || process.env.MANGO_RELEASE_MAVEN_CONSUME_SERVER_ID || 'mango-release-consume';
if (manifest.maven) {
  assertRegistryUrl(mavenPublishRegistry, 'Maven publish registry');
  assertRegistryUrl(mavenConsumeRegistry, 'Maven consume registry');
  if (!mavenPublishServerId) throw new Error('Maven publish server id is required');
}
const visibilityTimeoutSeconds = numericArg('--visibility-timeout', 300, 0, 300);
const visibilityPollSeconds = numericArg('--visibility-poll', 5, 1, 30);

if (action === 'repair' && manifest.status === 'COMPLETED') {
  verifyCompletedReleaseReadOnly();
  if (recoverRemoteWriteAudit(manifest)) writeManifest();
  printStatus(manifest);
  process.exit(0);
}

if (recoverRemoteWriteAudit(manifest)) writeManifest();

assertCleanWorktree(repoRoot);
runChecked(process.execPath, [join(workspaceRoot, 'scripts/release/check-release-plan.mjs')], repoRoot);
verifySourceAndArtifacts();
if (manifest.status === 'PREPARED') throw new Error('candidate verification has not passed; run mango release prepare');
if (manifest.status === 'FAILED' && manifest.states.CANDIDATE_VERIFIED?.status !== 'passed') {
  throw new Error('candidate verification failed; a new prepare is required');
}

let publishedCount = 0;
if (manifest.maven) publishMavenBatch();
for (const packageName of plan.order) {
  const artifact = manifest.artifacts.find((entry) => entry.name === packageName);
  const coordinate = `${artifact.name}@${artifact.version}`;
  const publication = manifest.packagePublications[packageName];
  const hosted = registryVersion(coordinate, publishRegistry);
  const group = registryVersion(coordinate, consumeRegistry);
  if (hosted.state === 'present') hosted.sha256 = registryTarballSha256(coordinate, publishRegistry);
  if (group.state === 'present') group.sha256 = registryTarballSha256(coordinate, consumeRegistry);
  const decision = decideRegistryAction({ hosted, consume: group, expectedSha256: artifact.sha256 });
  if (decision.action === 'STOP') failPublication(packageName, decision.reason, hosted, group);
  if (decision.action === 'VERIFIED') {
    publication.status = 'PUBLISHED';
    publication.hostedSha256 = hosted.sha256;
    publication.consumeSha256 = group.sha256;
  } else if (decision.action === 'VERIFY_PENDING') {
    publication.status = 'VERIFY_PENDING';
    publication.hostedSha256 = hosted.sha256;
  } else {
    const artifactPath = resolveArtifactPath(artifact);
    const startedAt = new Date().toISOString();
    markRemoteWriteIntent(manifest, { kind: 'npm-publish', target: coordinate, recordedAt: startedAt });
    writeManifest();
    const result = runCaptured(
      'npm',
      ['publish', artifactPath, `--registry=${publishRegistry}`, '--access=public'],
      repoRoot,
      20 * 60 * 1000,
    );
    publication.attempts.push(commandEvidence(result, startedAt, repoRoot));
    publication.updatedAt = new Date().toISOString();
    writeManifest();
    if (result.status !== 0) {
      publication.status = 'FAILED';
      manifest.status = publishedCount > 0 ? 'PARTIALLY_PUBLISHED' : 'FAILED';
      manifest.states.PUBLISHED = releaseState('failed', `npm publish failed for ${coordinate}`);
      writeManifest();
      throw new Error(`npm publish failed for ${coordinate}; inspect ${manifestPath}`);
    }
    const hostedHash = registryTarballSha256(coordinate, publishRegistry);
    if (hostedHash !== artifact.sha256) {
      failPublication(packageName, 'published hosted tarball hash differs from the sealed artifact');
    }
    publication.status = 'VERIFY_PENDING';
    publication.hostedSha256 = hostedHash;
  }

  const groupReady = publication.status === 'PUBLISHED' || waitForConsumeRegistry(coordinate, artifact.sha256);
  if (!groupReady) {
    publication.status = 'VERIFY_PENDING';
    manifest.status = 'VERIFY_PENDING';
    manifest.states.PUBLISHED = releaseState(
      'pending',
      `${coordinate} is published but not yet visible from consume registry`,
    );
    writeManifest();
    throw new Error(`${coordinate} is awaiting consume-registry visibility; run repair without republishing`);
  }
  publication.status = 'PUBLISHED';
  publication.consumeSha256 = artifact.sha256;
  publication.updatedAt = new Date().toISOString();
  publishedCount += 1;
  manifest.status = publishedCount === plan.order.length ? 'PUBLISHED' : 'PARTIALLY_PUBLISHED';
  writeManifest();
}

manifest.status = 'PUBLISHED';
manifest.states.PUBLISHED = releaseState(
  'passed',
  `all sealed ${plan.order.length} npm tarball(s)${manifest.maven ? ` and ${manifest.maven.coordinateCount} Maven coordinate(s)` : ''} resolve identically from publish and consume registries`,
);
writeManifest();

if (manifest.states.CONSUMER_VERIFIED.status !== 'passed') {
  const startedAt = new Date().toISOString();
  const npmConsumer =
    plan.order.length > 0
      ? runCaptured(
          'pnpm',
          [
            'package-consumer:typecheck',
            '--',
            '--release-candidate-matrix',
            '--pure-registry',
            '--reuse-build',
            `--registry=${consumeRegistry}`,
          ],
          workspaceRoot,
          60 * 60 * 1000,
        )
      : null;
  const mavenConsumer = manifest.maven ? verifyMavenConsumer() : [];
  manifest.evidence.consumer = {
    npm: npmConsumer ? commandEvidence(npmConsumer, startedAt, workspaceRoot) : null,
    maven: mavenConsumer,
  };
  if ((npmConsumer && npmConsumer.status !== 0) || mavenConsumer.some((entry) => entry.exitCode !== 0)) {
    manifest.status = 'FAILED';
    manifest.states.CONSUMER_VERIFIED = releaseState('failed', 'pure consume-registry consumer failed');
    writeManifest();
    throw new Error(`pure consume-registry consumer failed; immutable packages remain published: ${manifestPath}`);
  }
  manifest.status = 'CONSUMER_VERIFIED';
  manifest.states.CONSUMER_VERIFIED = releaseState(
    'passed',
    'clean consumer installed only the consume-registry tuple',
  );
  writeManifest();
}

completeTagAndRelease();
manifest.status = 'COMPLETED';
manifest.states.COMPLETED = releaseState(
  'passed',
  `tag and GitHub Release verified after consumer success: ${plan.release.tag}`,
);
manifest.nextBaseline = {
  schemaVersion: 1,
  tag: plan.release.tag,
  commit: manifest.source.commit,
  tree: manifest.source.tree,
  planDigest: plan.planDigest,
  packages: Object.fromEntries(plan.packages.map((entry) => [entry.name, entry.targetVersion])),
  maven: plan.maven?.targetVersion ?? null,
};
writeJson(join(releaseRoot, 'release-baseline.json'), manifest.nextBaseline);
applyCloseoutProjection();
writeManifest();
printStatus(manifest);

function verifySourceAndArtifacts() {
  if (manifest.schemaVersion !== 1 || manifest.planDigest !== plan.planDigest) {
    throw new Error('prepare manifest and release plan differ');
  }
  const headCommit = gitValue(repoRoot, ['rev-parse', 'HEAD^{commit}']);
  const headTree = gitValue(repoRoot, ['rev-parse', 'HEAD^{tree}']);
  const mainTree = gitValue(repoRoot, ['rev-parse', 'origin/main^{tree}']);
  if (headTree !== mainTree) throw new Error('publish requires HEAD tree to equal origin/main tree');
  if (headTree !== manifest.source.tree) throw new Error('merged source tree differs from the prepared source tree');
  if (headCommit !== manifest.source.commit) manifest.source.mergedCommit = headCommit;
  const notesPath = join(workspaceRoot, plan.release.notesFile);
  if (sha256(Buffer.from(readFileSync(notesPath, 'utf8'), 'utf8')) !== plan.release.notesSha256) {
    throw new Error('release notes changed after planning');
  }
  for (const artifact of manifest.artifacts) {
    const path = resolveArtifactPath(artifact);
    if (!existsSync(path) || sha256File(path) !== artifact.sha256) {
      throw new Error(`sealed artifact changed after prepare: ${artifact.name}@${artifact.version}`);
    }
  }
  if (manifest.maven) {
    verifyStagedMavenRepository(resolve(releaseRoot, manifest.maven.repository), manifest.maven);
  }
}

function publishMavenBatch() {
  const repository = resolve(releaseRoot, manifest.maven.repository);
  for (const coordinate of manifest.maven.coordinates) {
    const publication = manifest.mavenPublications[coordinate.coordinate];
    const publishFiles = coordinate.files.map((file) => remoteMavenFileState(mavenPublishRegistry, file));
    const consumeFiles = coordinate.files.map((file) => remoteMavenFileState(mavenConsumeRegistry, file));
    const decision = decideMavenCoordinateAction({ publishFiles, consumeFiles, expectedFiles: coordinate.files });
    if (decision.action === 'STOP')
      failMavenPublication(coordinate.coordinate, decision.reason, publishFiles, consumeFiles);
    if (decision.action === 'VERIFIED') {
      publication.status = 'PUBLISHED';
    } else if (decision.action === 'VERIFY_PENDING') {
      publication.status = 'VERIFY_PENDING';
    } else {
      const pom = coordinate.files.find((file) => file.path.endsWith('.pom'));
      const main = coordinate.packaging === 'jar' ? coordinate.files.find((file) => file.path.endsWith('.jar')) : pom;
      const startedAt = new Date().toISOString();
      markRemoteWriteIntent(manifest, {
        kind: 'maven-deploy',
        target: coordinate.coordinate,
        recordedAt: startedAt,
      });
      writeManifest();
      const result = runCaptured(
        'mvn',
        [
          '-q',
          'org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file',
          `-Dfile=${join(repository, main.path)}`,
          `-DpomFile=${join(repository, pom.path)}`,
          `-DgroupId=${coordinate.groupId}`,
          `-DartifactId=${coordinate.artifactId}`,
          `-Dversion=${coordinate.version}`,
          `-Dpackaging=${coordinate.packaging}`,
          '-DgeneratePom=false',
          `-DrepositoryId=${mavenPublishServerId}`,
          `-Durl=${mavenPublishRegistry}`,
        ],
        repoRoot,
        20 * 60 * 1000,
      );
      publication.attempts.push(commandEvidence(result, startedAt, repoRoot));
      writeManifest();
      if (result.status !== 0) {
        publication.status = 'FAILED';
        manifest.status = hasPublishedCoordinate() ? 'PARTIALLY_PUBLISHED' : 'FAILED';
        manifest.states.PUBLISHED = releaseState('failed', `Maven deploy failed for ${coordinate.coordinate}`);
        writeManifest();
        throw new Error(`Maven deploy failed for ${coordinate.coordinate}; inspect ${manifestPath}`);
      }
      const hostedAfter = coordinate.files.map((file) => remoteMavenFileState(mavenPublishRegistry, file));
      const hostedDecision = decideMavenCoordinateAction({
        publishFiles: hostedAfter,
        consumeFiles: coordinate.files.map((file) => ({ path: file.path, state: 'absent' })),
        expectedFiles: coordinate.files,
      });
      if (!['VERIFY_PENDING', 'VERIFIED'].includes(hostedDecision.action)) {
        failMavenPublication(
          coordinate.coordinate,
          'published Maven files differ from the sealed coordinate',
          hostedAfter,
          [],
        );
      }
      publication.status = 'VERIFY_PENDING';
    }
    if (publication.status !== 'PUBLISHED' && !waitForMavenConsume(coordinate)) {
      publication.status = 'VERIFY_PENDING';
      manifest.status = 'VERIFY_PENDING';
      manifest.states.PUBLISHED = releaseState(
        'pending',
        `${coordinate.coordinate} is awaiting Maven consume-registry visibility`,
      );
      writeManifest();
      throw new Error(
        `${coordinate.coordinate} is awaiting Maven consume-registry visibility; run repair without rebuilding`,
      );
    }
    publication.status = 'PUBLISHED';
    publication.updatedAt = new Date().toISOString();
    writeManifest();
  }
}

function remoteMavenFileState(registry, file) {
  const temporary = mkdtempSync(join(tmpdir(), 'mango-release-maven-'));
  try {
    const destination = join(temporary, 'artifact');
    const url = `${registry.replace(/\/$/u, '')}/${file.path}`;
    const result = runCaptured(
      'curl',
      ['-sS', '-L', '--max-time', '60', '-o', destination, '-w', '%{http_code}', url],
      repoRoot,
      70_000,
    );
    const statusCode = result.stdout.trim().slice(-3);
    if (result.status !== 0) return { path: file.path, state: 'unknown', output: result.stderr.trim() };
    if (statusCode === '404') return { path: file.path, state: 'absent' };
    if (statusCode !== '200') return { path: file.path, state: 'unknown', output: `HTTP ${statusCode}` };
    return { path: file.path, state: 'present', sha256: sha256File(destination) };
  } finally {
    rmSync(temporary, { recursive: true, force: true });
  }
}

function waitForMavenConsume(coordinate) {
  const deadline = Date.now() + visibilityTimeoutSeconds * 1000;
  for (;;) {
    const consumeFiles = coordinate.files.map((file) => remoteMavenFileState(mavenConsumeRegistry, file));
    const decision = decideMavenCoordinateAction({
      publishFiles: coordinate.files.map((file) => ({ ...file, state: 'present' })),
      consumeFiles,
      expectedFiles: coordinate.files,
    });
    if (decision.action === 'VERIFIED') return true;
    if (decision.action === 'STOP') throw new Error(`${coordinate.coordinate}: ${decision.reason}`);
    if (Date.now() >= deadline) return false;
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, visibilityPollSeconds * 1000);
  }
}

function verifyMavenConsumer() {
  const localRepository = mkdtempSync(join(tmpdir(), 'mango-release-maven-consumer-'));
  try {
    return manifest.maven.coordinates.map((entry) => {
      const coordinate = entry.packaging === 'pom' ? `${entry.coordinate}:pom` : entry.coordinate;
      const startedAt = new Date().toISOString();
      const result = runCaptured(
        'mvn',
        [
          '-q',
          '-U',
          'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get',
          `-Dmaven.repo.local=${localRepository}`,
          `-DremoteRepositories=${mavenConsumeServerId}::default::${mavenConsumeRegistry}`,
          `-Dartifact=${coordinate}`,
          '-Dtransitive=false',
        ],
        repoRoot,
        20 * 60 * 1000,
      );
      return commandEvidence(result, startedAt, repoRoot);
    });
  } finally {
    rmSync(localRepository, { recursive: true, force: true });
  }
}

function failMavenPublication(coordinate, reason, publishFiles = null, consumeFiles = null) {
  const publication = manifest.mavenPublications[coordinate];
  publication.status = 'FAILED';
  publication.reason = reason;
  publication.registryEvidence = { publishFiles, consumeFiles };
  manifest.status = hasPublishedCoordinate() ? 'PARTIALLY_PUBLISHED' : 'FAILED';
  manifest.states.PUBLISHED = releaseState('failed', `${coordinate}: ${reason}`);
  writeManifest();
  throw new Error(`${coordinate}: ${reason}`);
}

function hasPublishedCoordinate() {
  return [
    ...Object.values(manifest.mavenPublications ?? {}),
    ...Object.values(manifest.packagePublications ?? {}),
  ].some((entry) => entry.status === 'PUBLISHED' || entry.status === 'VERIFY_PENDING');
}

function applyCloseoutProjection() {
  const baselinePath = join(workspaceRoot, '.changeset/release-baseline.json');
  writeJson(baselinePath, manifest.nextBaseline);
  const removed = [];
  for (const file of [plan.legacyReconciliation?.file, ...(plan.changesets ?? []).map((entry) => entry.file)].filter(
    Boolean,
  )) {
    const path = resolve(workspaceRoot, file.replace(/^\.changeset\//u, '.changeset/'));
    if (!path.startsWith(`${join(workspaceRoot, '.changeset')}/`)) {
      throw new Error(`closeout path escapes .changeset: ${file}`);
    }
    if (existsSync(path)) {
      rmSync(path);
      removed.push(file);
    }
  }
  manifest.closeoutProjection = {
    status: 'PENDING_PR',
    baseline: '.changeset/release-baseline.json',
    removed,
    reason: 'submit the generated successful baseline and consumed intent cleanup through mango-submit-pr',
  };
}

function waitForConsumeRegistry(coordinate, expectedHash) {
  const deadline = Date.now() + visibilityTimeoutSeconds * 1000;
  for (;;) {
    const state = registryVersion(coordinate, consumeRegistry);
    if (state.state === 'unknown') throw new Error(`${coordinate}: consume registry state is unknown`);
    if (state.state === 'present') {
      const hash = registryTarballSha256(coordinate, consumeRegistry);
      if (hash !== expectedHash)
        throw new Error(`${coordinate}: consume registry tarball hash differs from sealed artifact`);
      return true;
    }
    if (Date.now() >= deadline) return false;
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, visibilityPollSeconds * 1000);
  }
}

function registryVersion(coordinate, registry) {
  const result = runCaptured(
    'npm',
    ['view', coordinate, 'version', `--registry=${registry}`, '--json'],
    repoRoot,
    60_000,
  );
  const output = `${result.stdout}\n${result.stderr}`;
  if (result.status === 0) {
    const value = JSON.parse(result.stdout);
    const version = Array.isArray(value) ? value.at(-1) : value;
    return { state: 'present', version: String(version), output: output.trim() };
  }
  if (/E404|404 Not Found|is not in this registry/iu.test(output)) return { state: 'absent', output: output.trim() };
  return { state: 'unknown', output: output.trim() };
}

function registryTarballSha256(coordinate, registry) {
  const temporary = mkdtempSync(join(tmpdir(), 'mango-release-registry-'));
  try {
    const result = runCaptured(
      'npm',
      ['pack', coordinate, `--registry=${registry}`, '--pack-destination', temporary],
      repoRoot,
      120_000,
    );
    if (result.status !== 0) throw new Error(`cannot download ${coordinate} from ${registry}: ${result.stderr}`);
    const tarballs = readdirSync(temporary).filter((file) => file.endsWith('.tgz'));
    if (tarballs.length !== 1)
      throw new Error(`expected one downloaded tarball for ${coordinate}, found ${tarballs.length}`);
    return sha256File(join(temporary, tarballs[0]));
  } finally {
    rmSync(temporary, { recursive: true, force: true });
  }
}

function completeTagAndRelease() {
  const tag = plan.release.tag;
  const intendedCommit = manifest.source.mergedCommit || manifest.source.commit;
  const localTag = runCaptured('git', ['rev-parse', '--verify', `refs/tags/${tag}^{commit}`], repoRoot);
  if (localTag.status === 0) {
    if (localTag.stdout.trim() !== intendedCommit) throw new Error(`existing tag ${tag} points to a different commit`);
  } else {
    runChecked('git', ['tag', '-a', tag, intendedCommit, '-m', plan.release.title], repoRoot);
  }
  const remoteTag = runCaptured('git', ['ls-remote', '--tags', 'origin', `refs/tags/${tag}^{}`], repoRoot);
  if (!remoteTag.stdout.trim()) {
    markRemoteWriteIntent(manifest, { kind: 'git-tag-push', target: tag });
    writeManifest();
    runChecked('git', ['push', 'origin', `refs/tags/${tag}`], repoRoot);
  }
  const release = runCaptured('gh', ['release', 'view', tag, '--json', 'tagName,url'], repoRoot);
  if (release.status !== 0) {
    markRemoteWriteIntent(manifest, { kind: 'github-release-create', target: tag });
    writeManifest();
    runChecked(
      'gh',
      [
        'release',
        'create',
        tag,
        '--verify-tag',
        '--title',
        plan.release.title,
        '--notes-file',
        join(workspaceRoot, plan.release.notesFile),
      ],
      repoRoot,
    );
  }
  const verified = runCaptured('gh', ['release', 'view', tag, '--json', 'tagName,url'], repoRoot);
  if (verified.status !== 0 || JSON.parse(verified.stdout).tagName !== tag)
    throw new Error(`GitHub Release verification failed: ${tag}`);
}

function verifyCompletedReleaseReadOnly() {
  for (const artifact of manifest.artifacts) {
    const coordinate = `${artifact.name}@${artifact.version}`;
    const hosted = registryVersion(coordinate, publishRegistry);
    const consume = registryVersion(coordinate, consumeRegistry);
    if (hosted.state === 'present') hosted.sha256 = registryTarballSha256(coordinate, publishRegistry);
    if (consume.state === 'present') consume.sha256 = registryTarballSha256(coordinate, consumeRegistry);
    const decision = decideRegistryAction({ hosted, consume, expectedSha256: artifact.sha256 });
    if (decision.action !== 'VERIFIED') {
      throw new Error(`${coordinate}: completed release registry verification failed: ${decision.reason}`);
    }
  }
  if (manifest.maven) {
    for (const coordinate of manifest.maven.coordinates) {
      const publishFiles = coordinate.files.map((file) => remoteMavenFileState(mavenPublishRegistry, file));
      const consumeFiles = coordinate.files.map((file) => remoteMavenFileState(mavenConsumeRegistry, file));
      const decision = decideMavenCoordinateAction({ publishFiles, consumeFiles, expectedFiles: coordinate.files });
      if (decision.action !== 'VERIFIED') {
        throw new Error(`${coordinate.coordinate}: completed Maven release verification failed: ${decision.reason}`);
      }
    }
  }
  const tag = plan.release.tag;
  const intendedCommit = manifest.source.mergedCommit || manifest.source.commit;
  const remoteTag = runCaptured('git', ['ls-remote', '--tags', 'origin', `refs/tags/${tag}^{}`], repoRoot);
  if (remoteTag.status !== 0 || remoteTag.stdout.trim().split(/\s+/u)[0] !== intendedCommit) {
    throw new Error(`completed release tag verification failed: ${tag}`);
  }
  const release = runCaptured('gh', ['release', 'view', tag, '--json', 'tagName,url'], repoRoot);
  if (release.status !== 0 || JSON.parse(release.stdout).tagName !== tag) {
    throw new Error(`completed GitHub Release verification failed: ${tag}`);
  }
}

function failPublication(packageName, reason, hosted = null, group = null) {
  const publication = manifest.packagePublications[packageName];
  publication.status = 'FAILED';
  publication.reason = reason;
  publication.registryEvidence = { hosted, group };
  manifest.status = hasPublishedCoordinate() ? 'PARTIALLY_PUBLISHED' : 'FAILED';
  manifest.states.PUBLISHED = releaseState('failed', `${packageName}: ${reason}`);
  writeManifest();
  throw new Error(`${packageName}: ${reason}`);
}

function resolveArtifactPath(artifact) {
  const path = resolve(releaseRoot, artifact.file);
  if (!path.startsWith(`${releaseRoot}/`)) throw new Error(`artifact path escapes release root: ${artifact.file}`);
  return path;
}

function printStatus(value) {
  console.log(`Release plan: ${value.planDigest}`);
  console.log(`Status: ${value.status}`);
  for (const name of value.packageOrder ?? []) {
    const artifact = value.artifacts.find((entry) => entry.name === name);
    console.log(`${name}@${artifact.version} ${value.packagePublications[name].status}`);
  }
  for (const coordinate of value.maven?.coordinates ?? []) {
    console.log(`${coordinate.coordinate} ${value.mavenPublications[coordinate.coordinate].status}`);
  }
}

function releaseState(status, reason) {
  return { status, reason, updatedAt: new Date().toISOString() };
}

function commandEvidence(result, startedAt, cwd) {
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

function writeManifest() {
  manifest.updatedAt = new Date().toISOString();
  writeJson(manifestPath, manifest);
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

function numericArg(name, fallback, minimum, maximum) {
  const raw = valueArg(name);
  const value = raw === '' ? fallback : Number(raw);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} must be an integer from ${minimum} to ${maximum}`);
  }
  return value;
}

function assertRegistryUrl(value, label) {
  if (!value) throw new Error(`${label} is required`);
  const url = new URL(value);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
    throw new Error(`${label} must be an HTTP(S) URL without credentials`);
  }
}
